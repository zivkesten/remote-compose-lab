package com.zivkesten.remotelab

import android.content.Context
import androidx.compose.remote.creation.compose.action.combinedAction
import androidx.compose.remote.creation.compose.action.hostAction
import androidx.compose.remote.creation.compose.action.valueChangeAction
import androidx.compose.remote.creation.compose.capture.captureSingleRemoteDocument
import androidx.compose.remote.creation.compose.capture.createCreationDisplayInfo
import androidx.compose.remote.creation.compose.layout.RemoteAlignment
import androidx.compose.remote.creation.compose.layout.RemoteArrangement
import androidx.compose.remote.creation.compose.layout.RemoteBox
import androidx.compose.remote.creation.compose.layout.RemoteCanvas
import androidx.compose.remote.creation.compose.layout.RemoteColumn
import androidx.compose.remote.creation.compose.layout.RemoteComposable
import androidx.compose.remote.creation.compose.layout.RemoteText
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.background
import androidx.compose.remote.creation.compose.modifier.clickable
import androidx.compose.remote.creation.compose.modifier.clip
import androidx.compose.remote.creation.compose.modifier.fillMaxSize
import androidx.compose.remote.creation.compose.modifier.padding
import androidx.compose.remote.creation.compose.modifier.size
import androidx.compose.remote.creation.compose.shapes.RemoteRoundedCornerShape
import androidx.compose.remote.creation.compose.state.MutableRemoteInt
import androidx.compose.remote.creation.compose.state.RemotePaint
import androidx.compose.remote.creation.compose.state.RemoteTimeVariables
import androidx.compose.remote.creation.compose.state.rc
import androidx.compose.remote.creation.compose.state.rdp
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.remote.creation.compose.state.rs
import androidx.compose.remote.creation.compose.state.rsp
import androidx.compose.remote.creation.compose.state.sin
import androidx.compose.remote.creation.compose.state.toRemoteString
import androidx.compose.remote.creation.compose.text.RemoteFontWeight
import androidx.compose.remote.creation.compose.text.RemoteTextAlign
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import java.text.DecimalFormat

enum class Template(val label: String) { CARD("Card"), COUNTER("Counter"), PULSE("Pulse") }

data class DocSpec(
    val template: Template = Template.CARD,
    val title: String = "Hello from a document",
    val subtitle: String = "This UI is data, not app code.",
    val accent: Long = 0xFF1E88E5,
    val version: Int = 1,
)

/** The "backend": turns a DocSpec into Remote Compose document bytes with the creation library. */
object DocFactory {
    suspend fun build(context: Context, spec: DocSpec, widthPx: Float, heightPx: Float): ByteArray {
        val info = createCreationDisplayInfo(context, Size(widthPx, heightPx))
        val accent = Color(spec.accent)
        val captured = captureSingleRemoteDocument(context = context, creationDisplayInfo = info) {
            when (spec.template) {
                Template.CARD -> CardDoc(spec, accent)
                Template.COUNTER -> CounterDoc(spec, accent)
                Template.PULSE -> PulseDoc(spec, accent)
            }
        }
        return captured.bytes
    }
}

@Composable
@RemoteComposable
private fun CardDoc(spec: DocSpec, accent: Color) {
    RemoteBox(
        modifier = RemoteModifier.fillMaxSize().background(accent.rc).padding(24.rdp),
        contentAlignment = RemoteAlignment.Center,
    ) {
        RemoteColumn(
            verticalArrangement = RemoteArrangement.spacedBy(12.rdp),
            horizontalAlignment = RemoteAlignment.CenterHorizontally,
        ) {
            RemoteText(text = spec.title.rs, color = Color.White.rc, fontSize = 30.rsp,
                fontWeight = RemoteFontWeight.Bold, textAlign = RemoteTextAlign.Center)
            RemoteText(text = spec.subtitle.rs, color = Color(0xCCFFFFFF).rc, fontSize = 16.rsp,
                textAlign = RemoteTextAlign.Center)
            RemoteText(text = "document version ${spec.version}".rs, color = Color(0x99FFFFFF).rc,
                fontSize = 12.rsp)
        }
    }
}

@Composable
@RemoteComposable
private fun CounterDoc(spec: DocSpec, accent: Color) {
    // State lives inside the document: tapping changes it with no app code and no network call.
    val count = remember { MutableRemoteInt(0) }
    RemoteBox(
        modifier = RemoteModifier.fillMaxSize().background(Color(0xFF101820).rc).padding(24.rdp),
        contentAlignment = RemoteAlignment.Center,
    ) {
        RemoteColumn(
            verticalArrangement = RemoteArrangement.spacedBy(14.rdp),
            horizontalAlignment = RemoteAlignment.CenterHorizontally,
        ) {
            RemoteText(text = spec.title.rs, color = Color.White.rc, fontSize = 22.rsp,
                fontWeight = RemoteFontWeight.Bold, textAlign = RemoteTextAlign.Center)
            RemoteText(text = count.toRemoteString(DecimalFormat("0")), color = accent.rc,
                fontSize = 64.rsp, fontWeight = RemoteFontWeight.Bold)
            RemoteBox(
                modifier = RemoteModifier.clip(RemoteRoundedCornerShape(24.rdp))
                    .background(accent.rc)
                    .clickable(
                        combinedAction(
                            valueChangeAction(count, count + 1),
                            hostAction(name = "counter_tapped".rs),
                        )
                    )
                    .padding(horizontal = 28.rdp, vertical = 14.rdp),
                contentAlignment = RemoteAlignment.Center,
            ) {
                RemoteText("Tap  +1".rs, color = Color.White.rc, fontSize = 18.rsp)
            }
            RemoteText(text = "${spec.subtitle} (v${spec.version})".rs, color = Color(0x99FFFFFF).rc,
                fontSize = 12.rsp, textAlign = RemoteTextAlign.Center)
        }
    }
}

@Composable
@RemoteComposable
private fun PulseDoc(spec: DocSpec, accent: Color) {
    RemoteBox(
        modifier = RemoteModifier.fillMaxSize().background(Color(0xFF0B1020).rc).padding(24.rdp),
        contentAlignment = RemoteAlignment.Center,
    ) {
        RemoteColumn(
            verticalArrangement = RemoteArrangement.spacedBy(10.rdp),
            horizontalAlignment = RemoteAlignment.CenterHorizontally,
        ) {
            // Animation is an expression the player evaluates every frame.
            RemoteCanvas(modifier = RemoteModifier.size(140.rdp)) {
                val paint = RemotePaint().apply { color = accent.rc }
                val wave = (sin(RemoteTimeVariables.continuousSeconds * 3f.rf) + 1f.rf) / 2f.rf
                drawCircle(paint = paint, radius = 30f.rf + 38f.rf * wave)
            }
            RemoteText(text = spec.title.rs, color = Color.White.rc, fontSize = 18.rsp,
                textAlign = RemoteTextAlign.Center)
            RemoteText(text = "${spec.subtitle} (v${spec.version})".rs, color = Color(0x99FFFFFF).rc,
                fontSize = 12.rsp, textAlign = RemoteTextAlign.Center)
        }
    }
}
