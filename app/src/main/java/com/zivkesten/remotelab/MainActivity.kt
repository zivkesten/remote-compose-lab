package com.zivkesten.remotelab

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.remote.player.view.RemoteComposePlayer
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

const val LOCAL_URL = "http://127.0.0.1:8765/doc.rc"
const val PLAYER_HEIGHT_DP = 320
const val FIRESTORE_URL =
    "https://firestore.googleapis.com/v1/projects/remote-compose-lab-1d079/databases/(default)/documents/demo/ui"

class MainActivity : ComponentActivity() {
    private val server = DocServer()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        server.start()
        setContent { MaterialTheme { LabScreen(server) } }
    }
}

@Composable
fun LabScreen(server: DocServer) {
    val context = LocalContext.current
    val density = context.resources.displayMetrics.density
    val widthPx = context.resources.displayMetrics.widthPixels.toFloat()
    val heightPx = PLAYER_HEIGHT_DP * density

    var spec by remember { mutableStateOf(DocSpec()) }
    var url by remember { mutableStateOf(LOCAL_URL) }
    var autoPoll by remember { mutableStateOf(true) }
    var status by remember { mutableStateOf("waiting for first document") }
    var lastAction by remember { mutableStateOf("-") }
    var loadNow by remember { mutableIntStateOf(0) }
    var etag by remember { mutableStateOf<String?>(null) }
    var loads by remember { mutableIntStateOf(0) }

    var cloudMode by remember { mutableStateOf(true) }
    var cloudStatus by remember { mutableStateOf("cloud: not checked yet") }

    // Cloud mode: poll the spec the web editor writes to Firestore, then re-author the document locally.
    LaunchedEffect(cloudMode) {
        var lastVersion = -1
        while (cloudMode) {
            val now = SimpleDateFormat("HH:mm:ss", Locale.US).format(Date())
            runCatching {
                withContext(Dispatchers.IO) {
                    val c = URL(FIRESTORE_URL).openConnection() as HttpURLConnection
                    c.connectTimeout = 5000
                    c.readTimeout = 5000
                    if (c.responseCode != 200) error("HTTP ${c.responseCode}")
                    val f = org.json.JSONObject(c.inputStream.readBytes().decodeToString()).getJSONObject("fields")
                    fun str(k: String) = f.getJSONObject(k).getString("stringValue")
                    DocSpec(
                        template = when (str("template")) {
                            "counter" -> Template.COUNTER
                            "pulse" -> Template.PULSE
                            else -> Template.CARD
                        },
                        title = str("title"),
                        subtitle = str("subtitle"),
                        accent = 0xFF000000L or str("accent").removePrefix("#").toLong(16),
                        version = f.getJSONObject("version").getString("integerValue").toInt(),
                    )
                }
            }.onSuccess {
                if (it.version != lastVersion) {
                    lastVersion = it.version
                    spec = it
                    cloudStatus = "cloud: applied spec v${it.version} at $now"
                } else cloudStatus = "cloud: no change, checked $now"
            }.onFailure { cloudStatus = "cloud: failed at $now: ${it.message}" }
            delay(2000)
        }
    }

    val player = remember {
        RemoteComposePlayer(context).also { p ->
            p.addIdActionListener { id, metadata ->
                lastAction = "host action id=$id metadata=$metadata"
                Toast.makeText(context, "Document called the app: $metadata", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // "Backend": every edit re-authors the document with the creation library and publishes it.
    LaunchedEffect(spec) {
        delay(250)
        server.bytes = DocFactory.build(context, spec, widthPx, heightPx)
        server.version = spec.version
    }

    // "Client": fetch the URL and, when the bytes changed, swap the document in the player.
    LaunchedEffect(url, autoPoll, loadNow) {
        while (true) {
            val result = runCatching {
                withContext(Dispatchers.IO) {
                    val c = URL(url).openConnection() as HttpURLConnection
                    c.connectTimeout = 4000
                    c.readTimeout = 4000
                    etag?.let { c.setRequestProperty("If-None-Match", it) }
                    when (c.responseCode) {
                        304 -> null
                        200 -> Pair(c.getHeaderField("ETag"), c.inputStream.readBytes())
                        else -> error("HTTP ${c.responseCode}")
                    }
                }
            }
            val now = SimpleDateFormat("HH:mm:ss", Locale.US).format(Date())
            result.onSuccess { r ->
                if (r != null) {
                    etag = r.first
                    player.setDocument(r.second)
                    loads++
                    status = "swapped document #$loads (${r.second.size} bytes) at $now"
                } else {
                    status = "unchanged, checked $now (loaded $loads so far)"
                }
            }.onFailure { status = "fetch failed at $now: ${it.message}" }
            if (!autoPoll) break
            delay(1000)
        }
    }

    Column(
        Modifier.fillMaxSize().background(Color(0xFFF4F6F8)).verticalScroll(rememberScrollState())
            .padding(top = 36.dp, bottom = 24.dp, start = 16.dp, end = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Remote Compose Lab", style = MaterialTheme.typography.headlineSmall)
        Text("androidx.compose.remote 1.0.0-alpha24", style = MaterialTheme.typography.labelMedium)

        Box(Modifier.fillMaxWidth().height(PLAYER_HEIGHT_DP.dp).clip(MaterialTheme.shapes.large)) {
            AndroidView(factory = { player }, modifier = Modifier.fillMaxSize())
        }
        Text(status, style = MaterialTheme.typography.bodySmall)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Switch(cloudMode, { cloudMode = it })
            Text("Follow the web editor (Firestore)")
        }
        Text(cloudStatus, style = MaterialTheme.typography.bodySmall)
        Text("Last callback from document: $lastAction", style = MaterialTheme.typography.bodySmall)

        Text("Player: where the document comes from", style = MaterialTheme.typography.titleSmall)
        OutlinedTextField(url, { url = it; etag = null }, Modifier.fillMaxWidth(), singleLine = true,
            label = { Text("Document URL (.rc file)") })
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Switch(autoPoll, { autoPoll = it })
            Text("Poll every second")
            Button({ etag = null; loadNow++ }) { Text("Load now") }
        }

        Text("Server: change the document live (no app release)", style = MaterialTheme.typography.titleSmall)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Template.entries.forEach { t ->
                FilterChip(spec.template == t, { spec = spec.copy(template = t, version = spec.version + 1) },
                    { Text(t.label) })
            }
        }
        OutlinedTextField(spec.title, { spec = spec.copy(title = it, version = spec.version + 1) },
            Modifier.fillMaxWidth(), label = { Text("Title") })
        OutlinedTextField(spec.subtitle, { spec = spec.copy(subtitle = it, version = spec.version + 1) },
            Modifier.fillMaxWidth(), label = { Text("Subtitle") })
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            listOf(0xFF1E88E5, 0xFF2E7D32, 0xFFD81B60, 0xFFF57C00, 0xFF6A1B9A).forEach { c ->
                Box(Modifier.size(40.dp).clip(CircleShape).background(Color(c))
                    .clickable { spec = spec.copy(accent = c, version = spec.version + 1) })
            }
        }
        Text(
            "Each edit re-authors the document with the creation library, serves it at $LOCAL_URL " +
                "from inside this app, and the player above picks it up and swaps it. " +
                "Point the URL at any host serving a .rc file to use a real server.",
            style = MaterialTheme.typography.bodySmall,
        )
    }
}
