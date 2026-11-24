# Remote Compose Lab

Small Android demo for `androidx.compose.remote` 1.0.0-alpha24 (Remote Compose).

- Creation side: `DocFactory.kt` builds a UI document (bytes) with the creation library.
- Player side: `RemoteComposePlayer` renders the bytes. The app fetches the document from a URL
  (default: a tiny server inside the app) and swaps it live when it changes. No app release needed.
- The Counter template keeps its state inside the document; the Pulse template animates by expression.

Needs Android 10+ (minSdk 29). Alpha library, API may change. Download the APK from the Releases page.
No personal data is used.

## Web editor (Firestore)

`docs/index.html` is a static page (served by GitHub Pages). It writes a small spec
(template, title, subtitle, accent) to Firestore document `demo/ui`. The app, with
"Follow the web editor" on, polls that document every 2 seconds, re-authors the Remote Compose
document with the creation library and swaps it in the player. Firestore rules allow public
read and only validated writes carrying the demo edit key. The key is not a real secret; this is a demo.
