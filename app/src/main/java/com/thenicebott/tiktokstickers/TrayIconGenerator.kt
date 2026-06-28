package com.thenicebott.tiktokstickers

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import java.io.File
import java.io.FileOutputStream

object TrayIconGenerator {

    private const val TRAY_SIZE = 96
    private const val MAX_TRAY_BYTES = 50 * 1024

    fun generateFromWebp(stickerWebpFile: File, outputPngFile: File): File {
        val source = decodeFirstFrameAsBitmap(stickerWebpFile)
            ?: throw IllegalStateException("No se pudo leer el sticker para generar el tray icon")

        val canvas = Bitmap.createBitmap(TRAY_SIZE, TRAY_SIZE, Bitmap.Config.ARGB_8888)
        val androidCanvas = Canvas(canvas)
        androidCanvas.drawColor(Color.TRANSPARENT)

        val scale = minOf(TRAY_SIZE.toFloat() / source.width, TRAY_SIZE.toFloat() / source.height)
        val w = (source.width * scale).toInt()
        val h = (source.height * scale).toInt()
        val scaled = Bitmap.createScaledBitmap(source, w, h, true)

        val left = (TRAY_SIZE - w) / 2f
        val top = (TRAY_SIZE - h) / 2f
        androidCanvas.drawBitmap(scaled, left, top, null)

        var quality = 100
        FileOutputStream(outputPngFile).use { out ->
            canvas.compress(Bitmap.CompressFormat.PNG, quality, out)
        }
        
        while (outputPngFile.length() > MAX_TRAY_BYTES && quality > 10) {
            quality -= 20
            FileOutputStream(outputPngFile).use { out ->
                canvas.compress(Bitmap.CompressFormat.PNG, quality, out)
            }
        }

        scaled.recycle()
        canvas.recycle()
        source.recycle()
        return outputPngFile
    }

    private fun decodeFirstFrameAsBitmap(file: File): Bitmap? {
        return BitmapFactory.decodeFile(file.absolutePath)
    }
}
