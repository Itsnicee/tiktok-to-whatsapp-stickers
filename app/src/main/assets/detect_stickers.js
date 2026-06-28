// detect_stickers.js
// Se inyecta en el WebView de TikTok vía evaluateJavascript() desde
// MainActivity.kt. A diferencia de la versión Electron, aquí no hay
// problema de "mundos" JS separados ni de Content Security Policy
// bloqueando inyección, porque WebView.evaluateJavascript() corre el
// código directamente en el contexto de la página sin pasar por un
// <script> tag insertado en el HTML.
//
// La comunicación hacia Kotlin se hace vía el puente
// @JavascriptInterface expuesto como "AndroidBridge" (ver MainActivity.kt).
//
// Misma señal de detección que en la versión Windows: TikTok marca cada
// sticker de chat con alt="sticker" en el <img>, sin importar las clases
// (que son hashes generados en build y cambian seguido).

(function () {
  if (window.__stickerGrabberInjected) {
    // Ya inyectado; solo re-ejecuta el escaneo por si el bridge lo pide de nuevo.
    return window.__scanForStickers();
  }
  window.__stickerGrabberInjected = true;

  const STICKER_ALT_VALUES = ['sticker'];
  const EXCLUDE_HINTS = ['avatar', 'profile', 'emoji-picker'];

  const SELECTOR_CANDIDATES = [
    'img[alt="sticker"]',
    '[data-e2e="chat-message"] img',
    '[data-e2e="chat-msg-list"] img',
    '.message-sticker img',
    '[class*="DivMessageContent"] img',
    '[class*="MessageContent"] img',
    '[class*="StickerImage"] img',
    'img[class*="StickerImage"]'
  ];

  function isLikelyAvatar(img) {
    if (STICKER_ALT_VALUES.includes((img.alt || '').toLowerCase())) {
      return false;
    }
    const haystack = `${img.className} ${img.alt || ''} ${img.closest('[class]')?.className || ''}`.toLowerCase();
    return EXCLUDE_HINTS.some((hint) => haystack.includes(hint));
  }

  function findStickerImages(root = document.body) {
    const found = new Set();
    for (const selector of SELECTOR_CANDIDATES) {
      root.querySelectorAll?.(selector)?.forEach((img) => found.add(img));
    }
    return [...found].filter((img) => !isLikelyAvatar(img));
  }

  // Expuesta en window para que MainActivity.kt pueda volver a llamarla
  // en cada click de "Escanear Conversación" sin reinyectar todo el script.
  window.__scanForStickers = function () {
    const images = findStickerImages();
    const urls = [...new Set(images.map((img) => img.src))];
    AndroidBridge.onStickersFound(JSON.stringify(urls));
    return urls.length;
  };

  window.__scanForStickers();
})();
