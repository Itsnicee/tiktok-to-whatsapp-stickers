package com.thenicebott.tiktokstickers

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.widget.Toast

object WhatsAppStickerLauncher {

    private const val ACTION_ENABLE_STICKER_PACK = "com.whatsapp.intent.action.ENABLE_STICKER_PACK"
    private const val EXTRA_STICKER_PACK_ID = "sticker_pack_id"
    private const val EXTRA_STICKER_PACK_AUTHORITY = "sticker_pack_authority"
    private const val EXTRA_STICKER_PACK_NAME = "sticker_pack_name"

    fun addPackToWhatsApp(activity: android.app.Activity, pack: StickerPack) {
        val authority = "${activity.packageName}.stickercontentprovider"

        val intent = Intent(ACTION_ENABLE_STICKER_PACK).apply {
            putExtra(EXTRA_STICKER_PACK_ID, pack.identifier)
            putExtra(EXTRA_STICKER_PACK_AUTHORITY, authority)
            putExtra(EXTRA_STICKER_PACK_NAME, pack.name)
        }

        intent.setPackage("com.whatsapp")
        try {
            activity.startActivityForResult(intent, 200)
            return
        } catch (e: ActivityNotFoundException) {
            
            intent.setPackage("com.whatsapp.w4b")
            try {
                activity.startActivityForResult(intent, 200)
                return
            } catch (e2: ActivityNotFoundException) {
                
                throw Exception("No se encontró WhatsApp ni WhatsApp Business en este dispositivo, o tu versión no soporta esta acción.")
            }
        }
    }
}
