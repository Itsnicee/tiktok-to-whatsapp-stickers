package com.thenicebott.tiktokstickers

import org.json.JSONArray
import org.json.JSONObject

data class StickerPack(
    val identifier: String,
    val name: String,
    val publisher: String,
    val trayImageFile: String,
    val stickers: List<StickerInPack>,
    val animatedStickerPack: Boolean
) {
    fun toJson(): JSONObject {
        val json = JSONObject()
        json.put("identifier", identifier)
        json.put("name", name)
        json.put("publisher", publisher)
        json.put("trayImageFile", trayImageFile)
        json.put("animatedStickerPack", animatedStickerPack)
        
        val stickersArray = JSONArray()
        for (sticker in stickers) {
            val stickerJson = JSONObject()
            stickerJson.put("imageFileName", sticker.imageFileName)
            val emojisArray = JSONArray()
            sticker.emojis.forEach { emojisArray.put(it) }
            stickerJson.put("emojis", emojisArray)
            stickersArray.put(stickerJson)
        }
        json.put("stickers", stickersArray)
        return json
    }

    companion object {
        fun fromJson(jsonString: String): StickerPack {
            val json = JSONObject(jsonString)
            val stickersArray = json.getJSONArray("stickers")
            val stickersList = mutableListOf<StickerInPack>()
            for (i in 0 until stickersArray.length()) {
                val stickerJson = stickersArray.getJSONObject(i)
                val emojisArray = stickerJson.optJSONArray("emojis")
                val emojisList = mutableListOf<String>()
                if (emojisArray != null) {
                    for (j in 0 until emojisArray.length()) {
                        emojisList.add(emojisArray.getString(j))
                    }
                } else {
                    emojisList.add("✨")
                }
                stickersList.add(
                    StickerInPack(
                        imageFileName = stickerJson.getString("imageFileName"),
                        emojis = emojisList
                    )
                )
            }
            return StickerPack(
                identifier = json.getString("identifier"),
                name = json.getString("name"),
                publisher = json.getString("publisher"),
                trayImageFile = json.getString("trayImageFile"),
                stickers = stickersList,
                animatedStickerPack = json.getBoolean("animatedStickerPack")
            )
        }
    }
}

data class StickerInPack(
    val imageFileName: String,
    val emojis: List<String> = listOf("✨")
)
