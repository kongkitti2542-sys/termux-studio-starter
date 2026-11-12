package dev.termuxstudio

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

private val bg = Color(0xFF101116)
private val panel = Color(0xFF20212D)
private val accent = Color(0xFF967BFF)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { Studio(this) }
    }
}

private class Bridge(private val token: String) {
    fun call(method: String, route: String, data: JSONObject? = null): JSONObject {
        val conn = URL("http://127.0.0.1:8765" + route).openConnection() as HttpURLConnection
        try {
            conn.requestMethod = method
            conn.connectTimeout = 4000
            conn.readTimeout = 6000
            conn.setRequestProperty("Authorization", "Bearer " + token)
            conn.setRequestProperty("Content-Type", "application/json")
            if (data != null) {
                conn.doOutput = true
                conn.outputStream.use { it.write(data.toString().toByteArray(Charsets.UTF_8)) }
            }
            val stream = if (conn.responseCode in 200..299) conn.inputStream else conn.errorStream
            val result = stream?.bufferedReader()?.use { it.readText() } ?: "{}"
            if (conn.responseCode !in 200..299) throw IllegalStateException("HTTP " + conn.responseCode + ": " + result)
            return JSONObject(result)
        } finally { conn.disconnect() }
    }
}

@Composable
private fun Studio(activity: ComponentActivity) {
    val prefs = remember { activity.getSharedPreferences("bridge", 0) }
    var token by remember { mutableStateOf(prefs.getString("token", "") ?: "") }
    var connected by remember { mutableStateOf(false) }
    var tab by remember { mutableStateOf("Home") }
    var status by remember { mutableStateOf("Bridge not connected") }
    var folder by remember { mutableStateOf(".") }
    var file by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var entries by remember { mutableStateOf(listOf<Pair<String, Boolean>>()) }
    var command by remember { mutableStateOf("") }
    var output by remember { mutableStateOf("") }
    var jobId by remember { mutableStateOf("") }
    var running by remember { mutableStateOf(false) }
    var confirmCommand by remember { mutableStateOf(false) }
    var codexPrompt by remember { mutableStateOf("") }
    var codexConversation by remember { mutableStateOf("") }
    var codexBusy by remember { mutableStateOf(false) }
    var codexJob by remember { mutableStateOf("") }
    val bridge = remember(token) { Bridge(token) }
    fun runTask(action: () -> Unit) {
        Thread {
            try { action() } catch (e: Exception) { status = e.message ?: "Connection failed"; connected = false }
        }.start()
    }
    fun refreshFiles() = runTask {
        val json = bridge.call("GET", "/v1/files?path=" + java.net.URLEncoder.encode(folder, "UTF-8"))
        val arr = json.getJSONArray("entries")
        entries = (0 until arr.length()).map {
            val item = arr.getJSONObject(it)
            item.getString("name") to item.getBoolean("directory")
        }
    }
    fun pollJob(id: String) = runTask {
        while (true) {
            val data = bridge.call("GET", "/v1/jobs/" + id)
            output = data.optString("output")
            running = data.optBoolean("running")
            if (!running) { status = "Exit: " + data.optString("exitCode"); break }
            Thread.sleep(600)
        }
    }
    MaterialTheme(colorScheme = darkColorScheme(primary = accent, background = bg, surface = panel)) {
        Column(Modifier.fillMaxSize().background(bg).windowInsetsPadding(WindowInsets.safeDrawing)) {
            Text("〉_  Termux Studio", color = Color.White, fontSize = 23.sp, modifier = Modifier.padding(16.dp))
            Text(if (connected) "● Bridge connected" else "○ Bridge offline", color = if (connected) Color(0xFF8DDBAA) else Color.LightGray, modifier = Modifier.padding(horizontal = 16.dp))
            HorizontalDivider(Modifier.padding(top = 12.dp))
            Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp)) {
                when (tab) {
                    "Home" -> {
                        Text("Local bridge pairing", color = Color.White, fontSize = 20.sp)
                        Spacer(Modifier.height(12.dp))
                        OutlinedTextField(value = token, onValueChange = { token = it }, label = { Text("Paste bridge token") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                        Spacer(Modifier.height(8.dp))
                        Button(onClick = {
                            prefs.edit().putString("token", token).apply()
                            runTask { bridge.call("GET", "/v1/health"); connected = true; status = "Bridge ready" }
                        }, enabled = token.length >= 32) { Text("Connect") }
                        Spacer(Modifier.height(16.dp))
                        Text("Run the bridge in Termux first. Keep the pairing token private.", color = Color.LightGray)
                    }
                    "Files" -> {
                        Text("Explorer", color = Color.White, fontSize = 20.sp)
                        OutlinedTextField(value = folder, onValueChange = { folder = it }, label = { Text("Workspace folder (relative to Termux home)") }, modifier = Modifier.fillMaxWidth())
                        Button(onClick = { refreshFiles() }, enabled = connected) { Text("Browse") }
                        entries.forEach { (name, directory) ->
                            TextButton(onClick = {
                                val target = if (folder == ".") name else "$folder/$name"
                                if (directory) { folder = target; refreshFiles() }
                                else runTask {
                                    content = bridge.call("GET", "/v1/file?path=" + java.net.URLEncoder.encode(target, "UTF-8")).getString("content")
                                    file = target
                                }
                            }) { Text((if (directory) "▸ " else "  ") + name) }
                        }
                        if (file.isNotEmpty()) {
                            Text("Editing: $file", color = Color.White)
                            OutlinedTextField(value = content, onValueChange = { content = it }, modifier = Modifier.fillMaxWidth().heightIn(min = 220.dp), textStyle = androidx.compose.ui.text.TextStyle(fontFamily = FontFamily.Monospace, fontSize = 13.sp))
                            Button(onClick = { runTask { bridge.call("PUT", "/v1/file", JSONObject().put("path", file).put("content", content)); status = "Saved $file" } }, enabled = connected) { Text("Save file") }
                        }
                    }
                    "AI" -> {
                        Text("Codex Chat", color = Color.White, fontSize = 20.sp)
                        Spacer(Modifier.height(8.dp))
                        Text("Uses your existing Codex CLI login. Each message starts a separate non-interactive job.", color = Color.LightGray, fontSize = 12.sp)
                        Text(codexConversation.ifEmpty { "Send your first message to Codex." }, color = Color(0xFFD5E2D8), fontFamily = FontFamily.Monospace, fontSize = 13.sp, modifier = Modifier.fillMaxWidth().background(Color(0xFF090C11), RoundedCornerShape(10.dp)).padding(14.dp))
                        OutlinedTextField(value = codexPrompt, onValueChange = { codexPrompt = it }, label = { Text("Message Codex") }, modifier = Modifier.fillMaxWidth().heightIn(min = 110.dp))
                        Row {
                            Button(onClick = {
                                val prompt = codexPrompt.trim()
                                if (prompt.isNotEmpty()) {
                                    codexPrompt = ""
                                    codexBusy = true
                                    codexConversation += "\\n\\nYou: " + prompt + "\\nCodex: "
                                    val prefix = codexConversation
                                    val quote = 39.toChar().toString()
                                    val escaped = quote + prompt.replace(quote, quote + 92.toChar() + quote + quote) + quote
                                    runTask {
                                        try {
                                            val response = bridge.call("POST", "/v1/jobs", JSONObject().put("command", "codex exec " + escaped).put("cwd", folder))
                                            codexJob = response.getString("id")
                                            while (true) {
                                                val job = bridge.call("GET", "/v1/jobs/" + codexJob)
                                                codexConversation = prefix + job.optString("output")
                                                if (!job.optBoolean("running")) {
                                                    codexBusy = false
                                                    status = "Codex exit: " + job.optString("exitCode")
                                                    break
                                                }
                                                Thread.sleep(650)
                                            }
                                        } catch (e: Exception) {
                                            codexBusy = false
                                            status = e.message ?: "Codex failed"
                                        }
                                    }
                                }
                            }, enabled = connected && !codexBusy && codexPrompt.isNotBlank()) { Text(if (codexBusy) "Working..." else "Send to Codex") }
                            Spacer(Modifier.width(8.dp))
                            OutlinedButton(onClick = { runTask { bridge.call("DELETE", "/v1/jobs/" + codexJob) } }, enabled = codexBusy && codexJob.isNotEmpty()) { Text("Stop") }
                        }
                    }
                    "Terminal", "Git" -> {
                        Text(when(tab) { "Git" -> "Git commands"; "AI" -> "Codex CLI"; else -> "Shell commands" }, color = Color.White, fontSize = 20.sp)
                        Spacer(Modifier.height(8.dp))
                        Text("One command per job; not a full PTY. Commands execute in Termux after confirmation.", color = Color.LightGray, fontSize = 12.sp)
                        OutlinedTextField(value = command, onValueChange = { command = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Command") }, textStyle = androidx.compose.ui.text.TextStyle(fontFamily = FontFamily.Monospace))
                        Row {
                            Button(onClick = { confirmCommand = true }, enabled = connected && command.isNotBlank() && !running) { Text("Run") }
                            Spacer(Modifier.width(8.dp))
                            OutlinedButton(onClick = { runTask { bridge.call("DELETE", "/v1/jobs/" + jobId); status = "Cancellation requested" } }, enabled = running && jobId.isNotEmpty()) { Text("Stop") }
                        }
                        Spacer(Modifier.height(12.dp))
                        Text(output.ifEmpty { "No output yet" }, color = Color(0xFFD5E2D8), fontFamily = FontFamily.Monospace, fontSize = 12.sp, modifier = Modifier.fillMaxWidth().background(Color(0xFF090C11), RoundedCornerShape(10.dp)).padding(14.dp))
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text(status, color = Color.LightGray, fontSize = 12.sp)
            }
            NavigationBar(containerColor = panel) {
                listOf("Home", "Files", "Terminal", "Git", "AI").forEach { item ->
                    NavigationBarItem(selected = tab == item, onClick = { tab = item }, icon = { Text(when(item) { "Home" -> "⌂"; "Files" -> "▣"; "Terminal" -> ">_"; "Git" -> "⑂"; else -> "✦" }) }, label = { Text(item, fontSize = 10.sp) })
                }
            }
        }
        if (confirmCommand) AlertDialog(
            onDismissRequest = { confirmCommand = false },
            title = { Text("Execute in Termux?") },
            text = { Text(command) },
            confirmButton = { TextButton(onClick = {
                confirmCommand = false
                val submitted = command
                runTask {
                    val response = bridge.call("POST", "/v1/jobs", JSONObject().put("command", submitted).put("cwd", folder))
                    jobId = response.getString("id")
                    running = true
                    output = ""
                    pollJob(jobId)
                }
            }) { Text("Execute") } },
            dismissButton = { TextButton(onClick = { confirmCommand = false }) { Text("Cancel") } }
        )
    }
}
