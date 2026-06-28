package com.thenicebott.tiktokstickers

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import java.io.File

class StickerThumbAdapter(
    private val urls: MutableList<String> = mutableListOf(),
    private val onSelectionChanged: (Int) -> Unit = {}
) : RecyclerView.Adapter<StickerThumbAdapter.ThumbViewHolder>() {

    val selectedUrls = mutableSetOf<String>()

    class ThumbViewHolder(val container: View) : RecyclerView.ViewHolder(container) {
        val imageView: ImageView = container.findViewById(R.id.thumbImage)
        val unselectedOverlay: View = container.findViewById(R.id.unselectedOverlay)
        val selectedBorder: View = container.findViewById(R.id.selectedBorder)
        val checkIcon: ImageView = container.findViewById(R.id.checkIcon)
    }

    fun submitList(newUrls: List<String>) {
        urls.clear()
        urls.addAll(newUrls)
        
        selectedUrls.clear()
        selectedUrls.addAll(newUrls)
        notifyDataSetChanged()
        onSelectionChanged(selectedUrls.size)
    }

    fun clear() {
        urls.clear()
        selectedUrls.clear()
        notifyDataSetChanged()
        onSelectionChanged(0)
    }

    override fun onCreateViewHolder(parent: ViewGroup, position: Int): ThumbViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_sticker_thumb, parent, false)
        return ThumbViewHolder(view)
    }

    override fun onBindViewHolder(holder: ThumbViewHolder, position: Int) {
        val url = urls[position]
        
        val isSelected = selectedUrls.contains(url)
        holder.unselectedOverlay.visibility = if (isSelected) View.GONE else View.VISIBLE
        holder.selectedBorder.visibility = if (isSelected) View.VISIBLE else View.GONE
        holder.checkIcon.visibility = if (isSelected) View.VISIBLE else View.GONE

        holder.container.setOnClickListener {
            if (selectedUrls.contains(url)) {
                selectedUrls.remove(url)
            } else {
                selectedUrls.add(url)
            }
            notifyItemChanged(position)
            onSelectionChanged(selectedUrls.size)
        }

        Thread {
            try {
                val bytes = java.net.URL(url).openStream().use { it.readBytes() }
                val bitmap = android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                holder.imageView.post { holder.imageView.setImageBitmap(bitmap) }
            } catch (e: Exception) {
                
            }
        }.start()
    }

    override fun getItemCount(): Int = urls.size
}
