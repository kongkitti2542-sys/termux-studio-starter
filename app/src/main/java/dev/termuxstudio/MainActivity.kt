package dev.termuxstudio

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val bg = Color(0xFF101116)
private val panel = Color(0xFF1D1F2A)
private val accent = Color(0xFF967BFF)
private val white = Color(0xFFF1EFFA)
private val muted = Color(0xFFADB0C2)

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
        Column(Modifier.fillMaxSize().background(bg).windowInsetsPadding(WindowInsets.safeDrawing)) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("〉_", color = accent, fontSize = 26.sp, fontFamily = FontFamily.Monospace)
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("Termux Studio", color = white, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    Text("IDE for Termux + Codex", color = muted, fontSize = 12.sp)
                }
            }
            HorizontalDivider(color = Color(0xFF30313E))
            Column(Modifier.weight(1f).fillMaxWidth().padding(16.dp)) {
                when (tab) {
                    "Home" -> {
                        SectionTitle("Workspace")
                        Text("Your mobile development environment", color = muted, fontSize = 13.sp)
                        Spacer(Modifier.height(18.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            DashboardTile("〉_", "Terminal", "Shell sessions", Modifier.weight(1f)) { tab = "Terminal" }
                            DashboardTile("</>", "Editor", "Source files", Modifier.weight(1f)) { tab = "Files" }
                        }
                        Spacer(Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            DashboardTile("✦", "Codex CLI", "Coding agent", Modifier.weight(1f)) { tab = "AI" }
                            DashboardTile("⑂", "Git", "Version control", Modifier.weight(1f)) { tab = "Git" }
                        }
                        Spacer(Modifier.height(26.dp))
                        SectionTitle("Recent projects")
                        InfoPanel("No projects connected", "A secure Termux bridge is required to browse your existing projects.")
                    }
                    "Files" -> {
                        SectionTitle("Explorer")
                        InfoPanel("No workspace open", "Termux file access is not configured. This screen does not read private app files.")
                        Spacer(Modifier.height(14.dp))
                        SectionTitle("Editor")
                        Surface(color = panel, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(16.dp)) {
                                Text("No file selected", color = white, fontFamily = FontFamily.Monospace)
                                Spacer(Modifier.height(10.dp))
                                Text("File tree, tabs and editing will activate after the approved file bridge is implemented.", color = muted, fontSize = 13.sp)
                            }
                        }
                    }
                    "Terminal" -> {
                        SectionTitle("Terminal")
                        Surface(color = Color(0xFF0A0D12), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth().weight(1f)) {
                            Column(Modifier.padding(16.dp)) {
                                Text("TERMUX STUDIO • SESSION OFFLINE", color = accent, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                                Spacer(Modifier.height(18.dp))
                                Text("$ connection pending", color = white, fontFamily = FontFamily.Monospace, fontSize = 14.sp)
                                Spacer(Modifier.height(8.dp))
                                Text("No shell is running. Commands cannot be executed until the local Termux transport passes security review.", color = muted, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                            }
                        }
                    }
                    "Git" -> {
                        SectionTitle("Source control")
                        InfoPanel("No repository selected", "Git operations require a connected project. No commands have been executed.")
                        Spacer(Modifier.height(14.dp))
                        SectionTitle("Changes")
                        InfoPanel("Working tree unavailable", "Diff, staging, commit and push will require explicit user actions.")
                    }
                    "AI" -> {
                        SectionTitle("Codex CLI")
                        InfoPanel("Agent disconnected", "Codex will run through an authorized Termux session. This app does not read ChatGPT tokens.")
                        Spacer(Modifier.height(14.dp))
                        SectionTitle("Session")
                        InfoPanel("No active session", "Login remains under the official Codex CLI authentication flow.")
                    }
                }
            }
            NavigationBar(containerColor = panel) {
                tabs.forEach { item ->
                    NavigationBarItem(
                        selected = tab == item,
                        onClick = { tab = item },
                        icon = { Text(when (item) { "Home" -> "⌂"; "Files" -> "▣"; "Terminal" -> ">_"; "Git" -> "⑂"; else -> "✦" }, color = if (tab == item) accent else muted, fontSize = 18.sp) },
                        label = { Text(item, fontSize = 11.sp) }
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(value: String) {
    Text(value, color = white, fontWeight = FontWeight.SemiBold, fontSize = 17.sp)
    Spacer(Modifier.height(10.dp))
}

@Composable
private fun DashboardTile(symbol: String, title: String, subtitle: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Surface(onClick = onClick, modifier = modifier.height(132.dp), color = panel, shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Text(symbol, color = accent, fontSize = 27.sp, fontFamily = FontFamily.Monospace)
            Column {
                Text(title, color = white, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(subtitle, color = muted, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun InfoPanel(title: String, body: String) {
    Surface(color = panel, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(title, color = white, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(7.dp))
            Text(body, color = muted, fontSize = 13.sp)
        }
    }
}
