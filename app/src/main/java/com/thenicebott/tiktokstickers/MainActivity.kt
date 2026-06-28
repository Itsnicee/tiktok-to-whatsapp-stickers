package com.thenicebott.tiktokstickers

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.io.File

class MainActivity : ComponentActivity() {

    private lateinit var webView: WebView
    private lateinit var stickerGrid: RecyclerView
    private lateinit var panelTitle: TextView
    private lateinit var btnScan: Button
    private lateinit var btnAddToWhatsApp: Button
    private lateinit var statusText: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var btnTogglePanel: ImageView
    private lateinit var adapter: StickerThumbAdapter

    private var detectedUrls: List<String> = emptyList()
    private var pendingSecondPack: StickerPack? = null
    private var isPanelExpanded = true

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        webView = findViewById(R.id.webView)
        stickerGrid = findViewById(R.id.stickerGrid)
        panelTitle = findViewById(R.id.panelTitle)
        btnScan = findViewById(R.id.btnScan)
        btnAddToWhatsApp = findViewById(R.id.btnAddToWhatsApp)
        statusText = findViewById(R.id.statusText)
        progressBar = findViewById(R.id.progressBar)
        btnTogglePanel = findViewById(R.id.btnTogglePanel)

        adapter = StickerThumbAdapter { count ->
            if (count == 0) {
                btnAddToWhatsApp.text = "Añadir a WhatsApp"
                btnAddToWhatsApp.isEnabled = false
            } else {
                btnAddToWhatsApp.text = "Añadir $count a WhatsApp"
                btnAddToWhatsApp.isEnabled = true
            }
        }
        stickerGrid.layoutManager = GridLayoutManager(this, 4)
        stickerGrid.adapter = adapter

        StickerPackRepository.loadPacks(this)

        setupWebView()

        btnScan.setOnClickListener { scanForStickers() }
        btnAddToWhatsApp.setOnClickListener {
            val pending = pendingSecondPack
            if (pending != null) {
                pendingSecondPack = null
                statusText.text = "Abriendo segundo paquete…"
                WhatsAppStickerLauncher.addPackToWhatsApp(this, pending)
            } else {
                processAndAddToWhatsApp()
            }
        }

        btnTogglePanel.setOnClickListener {
            togglePanel()
        }
        
        if (detectedUrls.isEmpty()) {
            isPanelExpanded = false
            updatePanelVisibility()
        }
    }

    private fun togglePanel() {
        isPanelExpanded = !isPanelExpanded
        updatePanelVisibility()
    }

    private fun updatePanelVisibility() {
        val visibility = if (isPanelExpanded) View.VISIBLE else View.GONE
        stickerGrid.visibility = visibility
        btnAddToWhatsApp.visibility = visibility
        statusText.visibility = visibility
        
        btnTogglePanel.rotation = if (isPanelExpanded) 0f else 180f
    }

    private fun setupWebView() {
        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        
        webView.settings.userAgentString = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
        webView.addJavascriptInterface(AndroidBridge(), "AndroidBridge")

        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                val scheme = request?.url?.scheme
                
                if (scheme != null && scheme != "http" && scheme != "https") {
                    return true 
                }
                return super.shouldOverrideUrlLoading(view, request)
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                
            }
        }

        webView.loadUrl("https://www.tiktok.com/messages")
    }

    private fun scanForStickers() {
        statusText.text = "Escaneando…"
        val script = assets.open("detect_stickers.js").bufferedReader().use { it.readText() }
        webView.evaluateJavascript(script) { }
    }

    private inner class AndroidBridge {
        @JavascriptInterface
        fun onStickersFound(urlsJson: String) {
            val urls = mutableListOf<String>()
            val array = JSONArray(urlsJson)
            for (i in 0 until array.length()) {
                urls.add(array.getString(i))
            }
            runOnUiThread {
                detectedUrls = urls
                adapter.submitList(urls)
                panelTitle.text = "Stickers detectados: ${urls.size}"
                btnAddToWhatsApp.isEnabled = urls.isNotEmpty()
                statusText.text = if (urls.isEmpty()) {
                    "No se encontraron stickers en pantalla. Abre un chat con stickers visibles."
                } else {
                    "${urls.size} sticker(s) encontrados."
                }
                
                if (urls.isNotEmpty() && !isPanelExpanded) {
                    togglePanel()
                }
            }
        }
    }

    private fun processAndAddToWhatsApp() {
        
        val urlsToProcess = detectedUrls.filter { adapter.selectedUrls.contains(it) }
        
        if (urlsToProcess.isEmpty()) return

        btnAddToWhatsApp.isEnabled = false
        statusText.text = "Convirtiendo stickers a formato WhatsApp…"
        progressBar.visibility = android.view.View.VISIBLE
        progressBar.max = urlsToProcess.size
        progressBar.progress = 0

        lifecycleScope.launch {
            try {
                android.util.Log.i("TikTokStickers", "Stickers a procesar: $urlsToProcess")
                val packs = withContext(Dispatchers.IO) {
                    buildStickerPacks(urlsToProcess) { current, total ->
                        lifecycleScope.launch(Dispatchers.Main) {
                            progressBar.progress = current
                            statusText.text = "Procesando $current de $total…"
                        }
                    }
                }

                if (packs.isEmpty()) {
                    statusText.text = "Se necesitan al menos 3 stickers del mismo tipo (estático o animado) para crear un paquete válido para WhatsApp."
                    return@launch
                }

                packs.forEach { StickerPackRepository.setPack(this@MainActivity, it) }

                if (packs.size == 1) {
                    statusText.text = "Listo. Abriendo WhatsApp…"
                    WhatsAppStickerLauncher.addPackToWhatsApp(this@MainActivity, packs.first())
                } else {
                    
                    pendingSecondPack = packs[1]
                    statusText.text = "Paquete 1/2 (${if (packs[0].animatedStickerPack) "animados" else "estáticos"}). " +
                        "Toca 'Añadir a WhatsApp' otra vez para el segundo paquete después de confirmar este."
                    WhatsAppStickerLauncher.addPackToWhatsApp(this@MainActivity, packs.first())
                }
            } catch (e: Exception) {
                statusText.text = "Error al convertir: ${e.message}"
            } finally {
                btnAddToWhatsApp.isEnabled = true
                progressBar.visibility = android.view.View.GONE
            }
        }
    }

    private suspend fun buildStickerPacks(
        urls: List<String>, 
        onProgressUpdate: (current: Int, total: Int) -> Unit
    ): List<StickerPack> {
        val staticEntries = mutableListOf<StickerInPack>()
        val animatedEntries = mutableListOf<StickerInPack>()
        var firstError: String? = null

        val uniqueSuffix = System.currentTimeMillis().toString()
        val staticId = "tiktok_static_$uniqueSuffix"
        val animatedId = "tiktok_animated_$uniqueSuffix"

        val staticDir = StickerPackRepository.getStickerPackDir(this, staticId)
        val animatedDir = StickerPackRepository.getStickerPackDir(this, animatedId)
        staticDir.mkdirs()
        animatedDir.mkdirs()

        urls.forEachIndexed { index, url ->
            try {
                
                val tempOutput = File(cacheDir, "tmp_sticker_$index.webp")
                val result = StickerProcessor.processStickerUrl(this, url, tempOutput)

                val targetDir = if (result.isAnimated) animatedDir else staticDir
                val finalName = "sticker_${if (result.isAnimated) animatedEntries.size else staticEntries.size}.webp"
                val finalFile = File(targetDir, finalName)
                tempOutput.copyTo(finalFile, overwrite = true)
                tempOutput.delete()

                if (result.isAnimated) {
                    animatedEntries.add(StickerInPack(imageFileName = finalName))
                } else {
                    staticEntries.add(StickerInPack(imageFileName = finalName))
                }
            } catch (e: Exception) {
                
                android.util.Log.e("TikTokStickers", "Error procesando sticker en $url", e)
                if (firstError == null) firstError = e.message ?: e.toString()
            } finally {
                onProgressUpdate(index + 1, urls.size)
            }
        }
        
        if (staticEntries.isEmpty() && animatedEntries.isEmpty() && firstError != null) {
            throw Exception(firstError)
        }

        val packs = mutableListOf<StickerPack>()
        val currentPacksCount = StickerPackRepository.getAllPacks().size
        val packNameSuffix = if (currentPacksCount > 0) " #${currentPacksCount + 1}" else ""

        if (staticEntries.size >= MIN_STICKERS_PER_PACK) {
            val trayFile = File(staticDir, "tray_icon.png")
            TrayIconGenerator.generateFromWebp(File(staticDir, staticEntries.first().imageFileName), trayFile)
            packs.add(
                StickerPack(
                    identifier = staticId,
                    name = "${getString(R.string.sticker_pack_name)}$packNameSuffix",
                    publisher = getString(R.string.sticker_pack_publisher),
                    trayImageFile = trayFile.name,
                    stickers = staticEntries.take(MAX_STICKERS_PER_PACK),
                    animatedStickerPack = false
                )
            )
        }

        if (animatedEntries.size >= MIN_STICKERS_PER_PACK) {
            val trayFile = File(animatedDir, "tray_icon.png")
            TrayIconGenerator.generateFromWebp(File(animatedDir, animatedEntries.first().imageFileName), trayFile)
            val suffixAnim = if (staticEntries.size >= MIN_STICKERS_PER_PACK) " #${currentPacksCount + 2}" else packNameSuffix
            packs.add(
                StickerPack(
                    identifier = animatedId,
                    name = "${getString(R.string.sticker_pack_name)} (animados)$suffixAnim",
                    publisher = getString(R.string.sticker_pack_publisher),
                    trayImageFile = trayFile.name,
                    stickers = animatedEntries.take(MAX_STICKERS_PER_PACK),
                    animatedStickerPack = true
                )
            )
        }

        return packs
    }

    companion object {
        
        private const val MIN_STICKERS_PER_PACK = 3
        private const val MAX_STICKERS_PER_PACK = 30
    }
}
