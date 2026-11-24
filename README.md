# Remote Compose Lab

Small Android demo for `androidx.compose.remote` 1.0.0-alpha24 (Remote Compose).

- Creation side: `DocFactory.kt` builds a UI document (bytes) with the creation library.
- Player side: `RemoteComposePlayer` renders the bytes. The app fetches the document from a URL
  (default: a tiny server inside the app) and swaps it live when it changes. No app release needed.
- The Counter template keeps its state inside the document; the Pulse template animates by expression.

Needs Android 10+ (minSdk 29). Alpha library, API may change. Download the APK from the Releases page.
No personal data is used.
