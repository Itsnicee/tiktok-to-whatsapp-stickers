# TikTok to WhatsApp Stickers 🚀

![Android](https://img.shields.io/badge/Android-3DDC84?style=for-the-badge&logo=android&logoColor=white)
![Kotlin](https://img.shields.io/badge/Kotlin-0095D5?style=for-the-badge&logo=kotlin&logoColor=white)

Una aplicación nativa para Android que permite descargar stickers desde TikTok y añadirlos directamente a WhatsApp.

## 🌟 Características

- **Extracción Directa:** Captura e importa stickers estáticos y animados desde TikTok.
- **Soporte de Stickers Animados:** Utiliza librerías nativas (`libwebp`) para procesar e integrar stickers animados en formato `.webp`, cumpliendo estrictamente con los requerimientos (dimensiones, tamaño y formato) de WhatsApp.
- **Integración Nativa One-Tap:** Instala los paquetes de stickers en WhatsApp de manera directa (sin apps intermedias ni exportar archivos manualmente), utilizando la API oficial (`StickerContentProvider` y los Intents de integración de WhatsApp).
- **Interfaz Fluida e Intuitiva:** Aplicación construida enteramente en Kotlin, utilizando buenas prácticas y programación asíncrona (Corrutinas).

## 🛠️ Tecnologías y Herramientas

- **Lenguaje:** Kotlin
- **Plataforma:** Android
- **Build System:** Gradle
- **Componentes Core:**
  - `ContentProvider` (Integración oficial con WA)
  - `Corrutinas` (Manejo asíncrono de descargas y procesamiento)
  - Encoders nativos de WebP (para stickers animados).

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

## 📦 Arquitectura Principal

- `MainActivity.kt`: Punto de entrada de la aplicación, maneja la interfaz principal y la gestión de URLs detectadas de TikTok.
- `StickerProcessor.kt`: Lógica de conversión de imágenes (PNG/WEBP crudo) al formato estándar de 512x512px exactos y límites de peso requeridos por WhatsApp.
- `StickerContentProvider.kt`: Provee el contenido del paquete de stickers a WhatsApp (cumpliendo con el contrato oficial de WA).
- `WhatsAppStickerLauncher.kt`: Inicia el flujo de instalación de un solo toque utilizando los Intents de WhatsApp (`ACTION_ENABLE_STICKER_PACK`).

## ⚠️ Notas de Uso Legal y Derechos

Este proyecto ha sido desarrollado con fines educativos y de portafolio. El contenido de los stickers pertenece a sus respectivos creadores en TikTok. Asegúrate de tener los derechos correspondientes al distribuir o utilizar contenido de terceros.
