package com.thenicebott.tiktokstickers

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import com.aureusapps.android.webpandroid.decoder.WebPDecoder
import com.aureusapps.android.webpandroid.decoder.WebPDecodeListener
import com.aureusapps.android.webpandroid.decoder.WebPInfo
import com.aureusapps.android.webpandroid.encoder.WebPAnimEncoder
import com.aureusapps.android.webpandroid.encoder.WebPAnimEncoderOptions
import com.aureusapps.android.webpandroid.encoder.WebPConfig
import com.aureusapps.android.webpandroid.encoder.WebPEncoder
import com.aureusapps.android.webpandroid.encoder.WebPMuxAnimParams
import com.aureusapps.android.webpandroid.encoder.WebPPreset
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import java.net.URL
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

object StickerProcessor {

    private const val TARGET_SIZE = 512
    private const val MAX_STATIC_BYTES = 100 * 1024
    private const val MAX_ANIMATED_BYTES = 500 * 1024
    private const val MAX_ANIMATED_DURATION_MS = 10_000L

    data class ProcessResult(val file: File, val isAnimated: Boolean)

    suspend fun processStickerUrl(context: Context, url: String, outputFile: File): ProcessResult {
        val rawBytes = downloadBytes(url)
        return processStickerBytes(context, rawBytes, outputFile)
    }

    private fun downloadBytes(url: String): ByteArray {
        val secureUrl = url.replace("http://", "https://")
        val connection = URL(secureUrl).openConnection() as java.net.HttpURLConnection
        
        connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
        connection.connect()
        return connection.inputStream.use { it.readBytes() }
    }

    private suspend fun processStickerBytes(context: Context, rawBytes: ByteArray, outputFile: File): ProcessResult {
        val tempInputFile = File(context.cacheDir, "raw_${System.currentTimeMillis()}.webp")
        tempInputFile.writeBytes(rawBytes)

        return try {
            val frames = decodeAllFrames(context, tempInputFile)
            tempInputFile.delete()

            val isAnimated = frames.size > 1
            val file = if (!isAnimated) {
                encodeStatic(context, frames.first().bitmap, outputFile)
            } else {
                encodeAnimated(context, frames, outputFile)
            }
            ProcessResult(file, isAnimated)
        } catch (e: Exception) {
            tempInputFile.delete()
            android.util.Log.e("TikTokStickers", "Error decodificando frames con libwebp", e)
            
            val fallbackBitmap = android.graphics.BitmapFactory.decodeByteArray(rawBytes, 0, rawBytes.size)
                ?: throw Exception("No es un WebP válido y tampoco se pudo decodificar como imagen estándar: ${e.message}")
            
            val file = encodeStatic(context, fallbackBitmap, outputFile)
            ProcessResult(file, false)
        }
    }

    private data class DecodedFrame(val bitmap: Bitmap, val timestampMs: Long)

    private suspend fun decodeAllFrames(context: Context, file: File): List<DecodedFrame> {
        val frames = mutableListOf<DecodedFrame>()
        val decoder = WebPDecoder(context)

        try {
            decoder.setDataSource(Uri.fromFile(file))
            val info = decoder.decodeInfo()
            android.util.Log.i("TikTokStickers", "WebPInfo frameCount: ${info.frameCount}, hasAnimation: ${info.hasAnimation}")
            
            var frameIndex = 0
            while (decoder.hasNextFrame()) {
                val frameResult = decoder.decodeNextFrame()
                val bitmap = frameResult.frame
                if (bitmap != null) {
                    frames.add(DecodedFrame(
                        bitmap.copy(bitmap.config ?: Bitmap.Config.ARGB_8888, false), 
                        frameResult.timestamp.toLong() 
                    ))
                    frameIndex++
                }
            }
            
            android.util.Log.i("TikTokStickers", "Decoded ${frames.size} frames")
            
            decoder.release()
            if (frames.isEmpty()) {
                throw IllegalStateException("No se pudo decodificar ningún frame del sticker")
            }
            return frames
        } catch (e: Exception) {
            decoder.release()
            throw e
        }
    }

    private fun resizeToStickerCanvas(source: Bitmap): Bitmap {
        val canvas = Bitmap.createBitmap(TARGET_SIZE, TARGET_SIZE, Bitmap.Config.ARGB_8888)
        val androidCanvas = android.graphics.Canvas(canvas)
        androidCanvas.drawColor(Color.TRANSPARENT)

        val scale = minOf(
            TARGET_SIZE.toFloat() / source.width,
            TARGET_SIZE.toFloat() / source.height
        )
        val scaledWidth = (source.width * scale).toInt()
        val scaledHeight = (source.height * scale).toInt()
        val scaled = Bitmap.createScaledBitmap(source, scaledWidth, scaledHeight, true)

        val left = (TARGET_SIZE - scaledWidth) / 2f
        val top = (TARGET_SIZE - scaledHeight) / 2f
        androidCanvas.drawBitmap(scaled, left, top, null)

        if (scaled !== source) scaled.recycle()
        return canvas
    }

    private suspend fun encodeStatic(context: Context, sourceBitmap: Bitmap, outputFile: File): File {
        val resized = resizeToStickerCanvas(sourceBitmap)

        val qualitySteps = listOf(80f, 65f, 50f, 35f, 20f)

        for (quality in qualitySteps) {
            val candidate = File(outputFile.parentFile, "${outputFile.nameWithoutExtension}_tmp.webp")
            encodeStaticAtQuality(context, resized, candidate, quality)
            if (candidate.length() <= MAX_STATIC_BYTES) {
                candidate.copyTo(outputFile, overwrite = true)
                candidate.delete()
                resized.recycle()
                return outputFile
            }
            candidate.delete()
        }

        encodeStaticAtQuality(context, resized, outputFile, qualitySteps.last())
        resized.recycle()
        return outputFile
    }

    private suspend fun encodeStaticAtQuality(context: Context, bitmap: Bitmap, outputFile: File, quality: Float) =
        suspendCancellableCoroutine<Unit> { cont ->
            val encoder = WebPEncoder(context, bitmap.width, bitmap.height)
            encoder.configure(
                config = WebPConfig(
                    lossless = WebPConfig.COMPRESSION_LOSSY,
                    quality = quality
                ),
                preset = WebPPreset.WEBP_PRESET_PICTURE
            )
            try {
                encoder.encode(bitmap, Uri.fromFile(outputFile))
                encoder.release()
                cont.resume(Unit)
            } catch (e: Exception) {
                encoder.release()
                cont.resumeWithException(e)
            }
        }

    private suspend fun encodeAnimated(context: Context, frames: List<DecodedFrame>, outputFile: File): File {
        val trimmedFrames = trimToMaxDuration(frames, MAX_ANIMATED_DURATION_MS)
        val resizedFrames = trimmedFrames.map { it.copy(bitmap = resizeToStickerCanvas(it.bitmap)) }

        val qualitySteps = listOf(75f, 50f, 30f, 15f, 5f)

        for (quality in qualitySteps) {
            val candidate = File(outputFile.parentFile, "${outputFile.nameWithoutExtension}_tmp.webp")
            encodeAnimatedAtQuality(context, resizedFrames, candidate, quality)
            if (candidate.length() <= MAX_ANIMATED_BYTES) {
                candidate.copyTo(outputFile, overwrite = true)
                candidate.delete()
                resizedFrames.forEach { it.bitmap.recycle() }
                return outputFile
            }
            candidate.delete()
        }

        resizedFrames.forEach { it.bitmap.recycle() }
        throw Exception("El sticker animado es demasiado pesado para WhatsApp incluso con máxima compresión.")
    }

    private fun trimToMaxDuration(frames: List<DecodedFrame>, maxDurationMs: Long): List<DecodedFrame> {
        if (frames.isEmpty()) return frames
        val firstTimestamp = frames.first().timestampMs
        val kept = frames.takeWhile { (it.timestampMs - firstTimestamp) <= maxDurationMs }
        return if (kept.isEmpty()) listOf(frames.first()) else kept
    }

    private suspend fun encodeAnimatedAtQuality(
        context: Context,
        frames: List<DecodedFrame>,
        outputFile: File,
        quality: Float
    ) = suspendCancellableCoroutine<Unit> { cont ->
        val first = frames.first()
        val encoder = WebPAnimEncoder(
            context = context,
            width = first.bitmap.width,
            height = first.bitmap.height,
            options = WebPAnimEncoderOptions(
                minimizeSize = true,
                animParams = WebPMuxAnimParams(
                    backgroundColor = Color.TRANSPARENT,
                    loopCount = 0 
                )
            )
        )
        encoder.configure(
            config = WebPConfig(
                lossless = WebPConfig.COMPRESSION_LOSSY,
                quality = quality
            ),
            preset = WebPPreset.WEBP_PRESET_PICTURE
        )

        try {
            val baseTimestamp = first.timestampMs
            for (frame in frames) {
                encoder.addFrame(frame.timestampMs - baseTimestamp, frame.bitmap)
            }
            val lastFrame = frames.last()
            
            val lastFrameDuration = if (frames.size > 1) {
                lastFrame.timestampMs - frames[frames.size - 2].timestampMs
            } else {
                33L
            }
            
            encoder.assemble(lastFrame.timestampMs - baseTimestamp + lastFrameDuration, Uri.fromFile(outputFile))
            encoder.release()
            cont.resume(Unit)
        } catch (e: Exception) {
            encoder.release()
            cont.resumeWithException(e)
        }
    }
}
