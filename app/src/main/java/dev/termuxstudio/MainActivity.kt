package dev.termuxstudio

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val bg = Color(0xFF101116)
private val panel = Color(0xFF20222E)
private val accent = Color(0xFF9378FF)
private val white = Color(0xFFF2F0FA)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { StudioApp() }
    }
}

@Composable
private fun StudioApp() {
    var tab by remember { mutableStateOf("Home") }
    val tabs = listOf("Home", "Files", "Terminal", "Git", "AI")
    MaterialTheme(colorScheme = darkColorScheme(primary = accent, background = bg, surface = panel, onSurface = white)) {
        Column(Modifier.fillMaxSize().background(bg)) {
            Text("Termux Studio", modifier = Modifier.padding(20.dp), color = white, fontSize = 25.sp)
            Text("IDE for Termux + Codex", modifier = Modifier.padding(horizontal = 20.dp), color = Color.LightGray)
            Spacer(Modifier.height(20.dp))
            Box(Modifier.weight(1f).fillMaxWidth().padding(horizontal = 16.dp)) {
                when (tab) {
                    "Home" -> Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        listOf("Terminal", "Editor", "Codex CLI", "Git").forEach { item ->
                            Button(onClick = { tab = when(item) { "Editor" -> "Files"; "Codex CLI" -> "AI"; else -> item } }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) { Text(item) }
                        }
                        Text("Projects will appear here after a secure Termux bridge is configured.", color = Color.LightGray)
                    }
                    "Files" -> LazyColumn { items(listOf("File explorer", "Project roots", "Editor tabs", "Read / write access pending integration")) { Text(it, Modifier.fillMaxWidth().padding(14.dp), color = white) } }
                    "Terminal" -> Text("Terminal emulator and session transport: not connected yet.", color = white)
                    "Git" -> Text("Git status / diff / commit: integration pending.", color = white)
                    "AI" -> Text("Codex CLI session: integration pending. No ChatGPT credentials are accessed by this app.", color = white)
                }
            }
            NavigationBar(containerColor = panel) {
                tabs.forEach { item ->
                    NavigationBarItem(selected = tab == item, onClick = { tab = item }, icon = { Text(when(item) { "Home" -> "⌂"; "Files" -> "▣"; "Terminal" -> ">_"; "Git" -> "⑂"; else -> "✦" }, color = white) }, label = { Text(item) })
                }
            }
        }
    }
}