package com.thenicebott.tiktokstickers

import android.content.ContentProvider
import android.content.ContentResolver
import android.content.ContentValues
import android.content.UriMatcher
import android.content.res.AssetFileDescriptor
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.os.ParcelFileDescriptor
import java.io.File
import java.io.FileNotFoundException

class StickerContentProvider : ContentProvider() {

    companion object {
        const val STICKERS = "stickers"
        const val STICKERS_ASSET = "stickers_asset"
        const val METADATA = "metadata"

        const val STICKER_PACK_IDENTIFIER_IN_QUERY = "sticker_pack_identifier"
        const val STICKER_PACK_NAME_IN_QUERY = "sticker_pack_name"
        const val STICKER_PACK_PUBLISHER_IN_QUERY = "sticker_pack_publisher"
        const val STICKER_PACK_ICON_IN_QUERY = "sticker_pack_icon"
        const val ANDROID_APP_DOWNLOAD_LINK_IN_QUERY = "android_play_store_link"
        const val IOS_APP_DOWNLOAD_LINK_IN_QUERY = "ios_app_download_link"
        const val PUBLISHER_EMAIL = "sticker_pack_publisher_email"
        const val PUBLISHER_WEBSITE = "sticker_pack_publisher_website"
        const val PRIVACY_POLICY_WEBSITE = "sticker_pack_privacy_policy_website"
        const val LICENSE_AGREEMENT_WEBSITE = "sticker_pack_license_agreement_website"
        const val IMAGE_DATA_VERSION = "image_data_version"
        const val AVOID_CACHE = "whatsapp_will_not_cache_stickers"
        const val ANIMATED_STICKER_PACK = "animated_sticker_pack"

        const val STICKER_FILE_NAME_IN_QUERY = "sticker_file_name"
        const val STICKER_FILE_EMOJI_IN_QUERY = "sticker_emoji"

        private const val STICKERS_CODE = 2
        private const val STICKERS_ASSET_CODE = 3
        private const val METADATA_CODE = 4
        private const val METADATA_CODE_FOR_SINGLE_PACK = 5

        private lateinit var AUTHORITY: String
        private lateinit var MATCHER: UriMatcher

        fun buildAuthorityUri(authority: String): Uri =
            Uri.Builder()
                .scheme(ContentResolver.SCHEME_CONTENT)
                .authority(authority)
                .appendPath(METADATA)
                .build()
    }

    override fun onCreate(): Boolean {
        AUTHORITY = "${context!!.packageName}.stickercontentprovider"
        StickerPackRepository.loadPacks(context!!)
        
        MATCHER = UriMatcher(UriMatcher.NO_MATCH).apply {
            addURI(AUTHORITY, METADATA, METADATA_CODE)
            addURI(AUTHORITY, "$METADATA/*", METADATA_CODE_FOR_SINGLE_PACK)
            addURI(AUTHORITY, STICKERS, STICKERS_CODE)
            addURI(AUTHORITY, "$STICKERS/*", STICKERS_CODE)
            addURI(AUTHORITY, "$STICKERS_ASSET/*/*", STICKERS_ASSET_CODE)
        }
        return true
    }

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?
    ): Cursor? {
        android.util.Log.d("TikTokStickers", "WhatsApp está consultando URI: $uri")
        return when (MATCHER.match(uri)) {
            METADATA_CODE -> getAllPacksInfoCursor()
            METADATA_CODE_FOR_SINGLE_PACK -> {
                val identifier = uri.lastPathSegment
                val pack = identifier?.let { StickerPackRepository.getPackByIdentifier(it) }
                if (pack != null) getAllPacksInfoCursor(listOf(pack)) else MatrixCursor(arrayOf())
            }
            STICKERS_CODE -> {
                val identifier = uri.lastPathSegment
                val pack = identifier?.let { StickerPackRepository.getPackByIdentifier(it) }
                if (pack != null) getStickersCursor(pack) else MatrixCursor(arrayOf())
            }
            else -> throw IllegalArgumentException("Unknown URI: $uri")
        }
    }

    private fun getAllPacksInfoCursor(packs: List<StickerPack> = StickerPackRepository.getAllPacks()): Cursor {
        val columns = arrayOf(
            STICKER_PACK_IDENTIFIER_IN_QUERY,
            STICKER_PACK_NAME_IN_QUERY,
            STICKER_PACK_PUBLISHER_IN_QUERY,
            STICKER_PACK_ICON_IN_QUERY,
            ANDROID_APP_DOWNLOAD_LINK_IN_QUERY,
            IOS_APP_DOWNLOAD_LINK_IN_QUERY,
            PUBLISHER_EMAIL,
            PUBLISHER_WEBSITE,
            PRIVACY_POLICY_WEBSITE,
            LICENSE_AGREEMENT_WEBSITE,
            IMAGE_DATA_VERSION,
            AVOID_CACHE,
            ANIMATED_STICKER_PACK
        )
        val cursor = MatrixCursor(columns)
        packs.forEach { pack ->
            cursor.addRow(
                arrayOf(
                    pack.identifier,
                    pack.name,
                    pack.publisher,
                    pack.trayImageFile,
                    "", 
                    "", 
                    "", 
                    "", 
                    "", 
                    "", 
                    "1", 
                    1, 
                    if (pack.animatedStickerPack) 1 else 0
                )
            )
        }
        return cursor
    }

    private fun getStickersCursor(pack: StickerPack): Cursor {
        val cursor = MatrixCursor(arrayOf(STICKER_FILE_NAME_IN_QUERY, STICKER_FILE_EMOJI_IN_QUERY))
        pack.stickers.forEach { sticker ->
            cursor.addRow(arrayOf(sticker.imageFileName, sticker.emojis.joinToString(",")))
        }
        return cursor
    }

    override fun openAssetFile(uri: Uri, mode: String): AssetFileDescriptor {
        android.util.Log.d("TikTokStickers", "WhatsApp solicita archivo: $uri")
        
        val segments = uri.pathSegments
        if (segments.size < 3) {
            throw FileNotFoundException("URI con formato inesperado: $uri")
        }
        val identifier = segments[segments.size - 2]
        val fileName = segments.last()

        val pack = StickerPackRepository.getPackByIdentifier(identifier)
            ?: throw FileNotFoundException("No hay sticker pack activo con identifier: $identifier")

        val dir = StickerPackRepository.getStickerPackDir(context!!, identifier)
        val file = File(dir, fileName)

        if (!file.exists()) {
            throw FileNotFoundException("WhatsApp pidió un archivo que no existe: $fileName")
        }

        val pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
        return AssetFileDescriptor(pfd, 0, file.length())
    }

    override fun getType(uri: Uri): String? = when (MATCHER.match(uri)) {
        METADATA_CODE -> "vnd.android.cursor.dir/vnd.${context!!.packageName}.$METADATA"
        METADATA_CODE_FOR_SINGLE_PACK -> "vnd.android.cursor.item/vnd.${context!!.packageName}.$METADATA"
        STICKERS_CODE -> "vnd.android.cursor.dir/vnd.${context!!.packageName}.$STICKERS"
        STICKERS_ASSET_CODE -> {
            val fileName = uri.lastPathSegment ?: return "application/octet-stream"
            if (fileName.endsWith(".png")) "image/png" else "image/webp"
        }
        else -> null
    }

    override fun insert(uri: Uri, values: ContentValues?): Uri? =
        throw UnsupportedOperationException("No se permite insertar vía este provider")

    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?): Int =
        throw UnsupportedOperationException("No se permite actualizar vía este provider")

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int =
        throw UnsupportedOperationException("No se permite borrar vía este provider")
}
