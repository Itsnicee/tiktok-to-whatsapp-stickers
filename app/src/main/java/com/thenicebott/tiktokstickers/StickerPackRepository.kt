package com.thenicebott.tiktokstickers

import android.content.Context
import java.io.File

object StickerPackRepository {

    private val packs = mutableListOf<StickerPack>()
    private var isLoaded = false

    fun getStickerPackDir(context: Context, identifier: String): File {
        val dir = File(context.filesDir, "stickerpacks/$identifier")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun loadPacks(context: Context) {
        if (isLoaded) return
        packs.clear()
        val packsDir = File(context.filesDir, "stickerpacks")
        if (packsDir.exists() && packsDir.isDirectory) {
            packsDir.listFiles()?.forEach { dir ->
                if (dir.isDirectory) {
                    val packFile = File(dir, "pack.json")
                    if (packFile.exists()) {
                        try {
                            val jsonString = packFile.readText()
                            val pack = StickerPack.fromJson(jsonString)
                            packs.add(pack)
                        } catch (e: Exception) {
                            android.util.Log.e("TikTokStickers", "Error loading pack ${dir.name}", e)
                        }
                    }
                }
            }
        }
        isLoaded = true
    }

    fun setPack(context: Context, pack: StickerPack) {
        
        packs.removeAll { it.identifier == pack.identifier }
        packs.add(pack)
        
        val dir = getStickerPackDir(context, pack.identifier)
        val packFile = File(dir, "pack.json")
        packFile.writeText(pack.toJson().toString())
    }

    fun getPackByIdentifier(identifier: String): StickerPack? {
        return packs.find { it.identifier == identifier }
    }

    fun getAllPacks(): List<StickerPack> = packs.toList()

    fun clear(context: Context) {
        packs.clear()
        File(context.filesDir, "stickerpacks").deleteRecursively()
        isLoaded = false
    }
}
