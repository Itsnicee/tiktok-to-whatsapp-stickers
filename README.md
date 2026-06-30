<div align="center">

<img src="https://capsule-render.vercel.app/api?type=waving&color=0:000000,100:434343&height=160&section=header&text=TikTok%20%E2%86%92%20WhatsApp%20Stickers&fontSize=38&fontColor=ffffff&fontAlignY=38" />

# TikTok to WhatsApp Stickers

[![Android](https://img.shields.io/badge/Android-black?style=for-the-badge&logo=android&logoColor=white)](https://github.com/Itsnicee/tiktok-to-whatsapp-stickers/releases/latest)
[![Kotlin](https://img.shields.io/badge/Kotlin-black?style=for-the-badge&logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Gradle](https://img.shields.io/badge/Gradle-black?style=for-the-badge&logo=gradle&logoColor=white)](https://gradle.org)
[![Release](https://img.shields.io/github/v/release/Itsnicee/tiktok-to-whatsapp-stickers?style=for-the-badge&color=black&label=Latest)](https://github.com/Itsnicee/tiktok-to-whatsapp-stickers/releases/latest)

Una aplicación nativa para Android que permite descargar stickers desde TikTok y añadirlos directamente a WhatsApp.

[![Download APK](https://img.shields.io/badge/⬇%20Download%20APK-black?style=for-the-badge)](https://github.com/Itsnicee/tiktok-to-whatsapp-stickers/releases/latest)

</div>

---

## 📸 Demo

<p align="center">
  <img width="220" src="https://github.com/user-attachments/assets/6ec39ff2-a43d-4d2d-96e2-6499bb9e3730" />
  <img width="220" src="https://github.com/user-attachments/assets/b44f610e-df1f-4a15-9fe7-abdf6492df4b" />
</p>


---

## 🌟 Características

- **Extracción Directa:** Captura e importa stickers estáticos y animados desde TikTok.
- **Soporte de Stickers Animados:** Utiliza librerías nativas (`libwebp`) para procesar e integrar stickers animados en formato `.webp`, cumpliendo estrictamente con los requerimientos (dimensiones, tamaño y formato) de WhatsApp.
- **Integración Nativa One-Tap:** Instala los paquetes de stickers en WhatsApp de manera directa (sin apps intermedias ni exportar archivos manualmente), utilizando la API oficial (`StickerContentProvider` y los Intents de integración de WhatsApp).
- **Interfaz Fluida e Intuitiva:** Aplicación construida enteramente en Kotlin, utilizando buenas prácticas y programación asíncrona (Corrutinas).

---

## 🛠️ Tecnologías y Herramientas

- **Lenguaje:** Kotlin
- **Plataforma:** Android
- **Build System:** Gradle
- **Componentes Core:**
  - `ContentProvider` (Integración oficial con WA)
  - `Corrutinas` (Manejo asíncrono de descargas y procesamiento)
  - Encoders nativos de WebP (para stickers animados).

---

## 🚀 Cómo empezar (Desarrollo)

### Requisitos Previos

- [Android Studio](https://developer.android.com/studio) (última versión recomendada).
- SDK de Android.
- Un dispositivo físico con WhatsApp instalado (o un emulador, aunque la integración de los stickers requiere la app oficial de WhatsApp instalada).

### Instalación local

1. Clona este repositorio:
   ```bash
   git clone https://github.com/Itsnicee/tiktok-to-whatsapp-stickers.git
   ```
2. Abre el proyecto en Android Studio.
3. Permite que Gradle sincronice todas las dependencias.
4. Compila y ejecuta la aplicación en tu dispositivo:
   - Haz clic en **Run** ▶️ en Android Studio, o en la terminal ejecuta:
     ```bash
     ./gradlew assembleDebug
     ```

---

## 📦 Arquitectura Principal

- `MainActivity.kt`: Punto de entrada de la aplicación, maneja la interfaz principal y la gestión de URLs detectadas de TikTok.
- `StickerProcessor.kt`: Lógica de conversión de imágenes (PNG/WEBP crudo) al formato estándar de 512x512px exactos y límites de peso requeridos por WhatsApp.
- `StickerContentProvider.kt`: Provee el contenido del paquete de stickers a WhatsApp (cumpliendo con el contrato oficial de WA).
- `WhatsAppStickerLauncher.kt`: Inicia el flujo de instalación de un solo toque utilizando los Intents de WhatsApp (`ACTION_ENABLE_STICKER_PACK`).

---

## ⚠️ Notas de Uso Legal y Derechos

Este proyecto ha sido desarrollado con fines educativos y de portafolio. El contenido de los stickers pertenece a sus respectivos creadores en TikTok. Asegúrate de tener los derechos correspondientes al distribuir o utilizar contenido de terceros.

---

<div align="center">

Made by [@Itsnicee](https://github.com/Itsnicee) · ZierowStudio

<img src="https://capsule-render.vercel.app/api?type=waving&color=0:434343,100:000000&height=100&section=footer" />

</div>
