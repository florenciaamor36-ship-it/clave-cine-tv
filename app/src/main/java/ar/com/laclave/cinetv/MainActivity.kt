package ar.com.laclave.cinetv

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

private val Ink = Color(0xFF0B0B0E)
private val Panel = Color(0xFF19191F)
private val Gold = Color(0xFFE5B847)
private val Muted = Color(0xFFAAAAB3)
private val Red = Color(0xFFE05A52)

private data class YoutubeVideo(
    val id: String,
    val title: String,
    val channel: String,
    val thumbnail: String?,
    val live: Boolean,
    val description: String,
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = getSharedPreferences("youtube_api_config", MODE_PRIVATE)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme(primary = Gold, background = Ink, surface = Panel, onPrimary = Ink, onBackground = Color.White, onSurface = Color.White)) {
                YoutubeCatalog(
                    getApiKey = { prefs.getString("api_key", "").orEmpty() },
                    saveApiKey = { prefs.edit().putString("api_key", it.trim()).apply() },
                    search = { key, query, live -> searchYoutube(key, query, live) },
                    openVideo = { id ->
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/watch?v=$id"))
                        startActivity(intent)
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun YoutubeCatalog(
    getApiKey: () -> String,
    saveApiKey: (String) -> Unit,
    search: suspend (String, String, Boolean) -> List<YoutubeVideo>,
    openVideo: (String) -> Unit,
) {
    var tab by remember { mutableIntStateOf(0) }
    var query by remember { mutableStateOf("") }
    var apiKey by remember { mutableStateOf(getApiKey()) }
    var apiInput by remember { mutableStateOf("") }
    var showSettings by remember { mutableStateOf(apiKey.isBlank()) }
    var results by remember { mutableStateOf(emptyList<YoutubeVideo>()) }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("Buscá películas públicas o canales en vivo de YouTube.") }
    val context = LocalContext.current

    fun launchSearch(live: Boolean = tab == 1) {
        if (apiKey.isBlank()) { showSettings = true; message = "Configurá una clave restringida de YouTube Data API para buscar."; return }
        val term = query.trim().ifBlank { if (live) "canales en vivo Argentina" else "películas completas" }
        busy = true
        message = if (live) "Buscando transmisiones activas…" else "Buscando en YouTube…"
        (context as? ComponentActivity)?.lifecycleScope?.launch {
            try {
                results = search(apiKey, term, live)
                message = if (results.isEmpty()) "No encontré resultados. Probá otra búsqueda." else "${results.size} resultados de YouTube"
            } catch (e: Exception) {
                message = e.message ?: "No pude consultar YouTube. Revisá la clave o la conexión."
                results = emptyList()
            } finally { busy = false }
        }
    }

    Scaffold(
        containerColor = Ink,
        topBar = {
            TopAppBar(
                title = { Column { Text("LA CLAVE", color = Gold, fontWeight = FontWeight.Black, letterSpacing = 2.sp, fontSize = 18.sp); Text("CINE · YOUTUBE", color = Muted, fontSize = 11.sp, letterSpacing = 1.4.sp) } },
                actions = { IconButton(onClick = { apiInput = apiKey; showSettings = true }) { Icon(Icons.Default.Settings, "Configurar API", tint = Gold) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Ink)
            )
        },
        bottomBar = {
            NavigationBar(containerColor = Panel) {
                listOf("Buscar", "En vivo", "Películas").forEachIndexed { i, label ->
                    NavigationBarItem(
                        selected = tab == i,
                        onClick = { tab = i; results = emptyList(); message = when(i) { 1 -> "Buscá transmisiones que estén en vivo ahora."; 2 -> "Buscá películas disponibles públicamente en YouTube."; else -> "Buscá videos y contenido público de YouTube." } },
                        icon = { Icon(when(i) { 1 -> Icons.Default.LiveTv; 2 -> Icons.Default.Movie; else -> Icons.Default.Search }, contentDescription = label) },
                        label = { Text(label) },
                        colors = NavigationBarItemDefaults.colors(selectedIconColor = Gold, selectedTextColor = Gold, indicatorColor = Color(0xFF332A18), unselectedIconColor = Muted, unselectedTextColor = Muted)
                    )
                }
            }
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Text(when(tab) { 1 -> "Canales en vivo de YouTube"; 2 -> "Películas en YouTube"; else -> "Explorá YouTube" }, Modifier.padding(start = 20.dp, end = 20.dp, top = 12.dp), color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Text("Catálogo de YouTube · reproducción oficial", Modifier.padding(horizontal = 20.dp, vertical = 4.dp), color = Muted, fontSize = 13.sp)
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = query, onValueChange = { query = it }, modifier = Modifier.weight(1f), singleLine = true,
                    placeholder = { Text(when(tab) { 1 -> "Ej. noticias Argentina"; 2 -> "Ej. película completa español"; else -> "Buscar en YouTube" }, color = Muted) },
                    leadingIcon = { Icon(Icons.Default.Search, null, tint = Muted) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Gold, unfocusedBorderColor = Color(0xFF393941), focusedTextColor = Color.White, unfocusedTextColor = Color.White, cursorColor = Gold)
                )
                Button(onClick = { launchSearch() }, enabled = !busy, colors = ButtonDefaults.buttonColors(containerColor = Gold, contentColor = Ink), contentPadding = PaddingValues(horizontal = 16.dp)) {
                    if (busy) CircularProgressIndicator(Modifier.size(20.dp), color = Ink, strokeWidth = 2.dp) else Icon(Icons.Default.Search, "Buscar")
                }
            }
            Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(message, Modifier.weight(1f), color = Muted, fontSize = 12.sp)
                Text("YouTube", color = Red, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
            if (results.isEmpty() && !busy) {
                Column(Modifier.fillMaxSize().padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    Icon(if(tab == 1) Icons.Default.LiveTv else Icons.Default.Movie, null, tint = Gold, modifier = Modifier.size(54.dp))
                    Spacer(Modifier.height(16.dp))
                    Text("Contenido desde YouTube", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Text(if (tab == 1) "Buscá transmisiones que estén activas en YouTube." else "Buscá videos y películas públicas. Al tocar un resultado, se abre en la app oficial de YouTube.", color = Muted, fontSize = 14.sp, lineHeight = 20.sp)
                    Spacer(Modifier.height(15.dp))
                    TextButton(onClick = { apiInput = apiKey; showSettings = true }) { Text("Configurar API de YouTube", color = Gold) }
                }
            } else {
                LazyColumn(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(results, key = { it.id }) { item ->
                        YoutubeRow(item) { openVideo(item.id) }
                    }
                }
            }
        }
    }

    if (showSettings) {
        AlertDialog(
            onDismissRequest = { if (apiKey.isNotBlank()) showSettings = false },
            title = { Text("Configurar búsqueda de YouTube") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Ingresá una clave de YouTube Data API v3. Creala en Google Cloud Console y restringila a Android y al paquete ar.com.laclave.cinetv. La clave no es la contraseña de tu cuenta Google.", color = Muted, fontSize = 13.sp)
                    OutlinedTextField(value = apiInput, onValueChange = { apiInput = it }, singleLine = true, label = { Text("API key") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Gold, unfocusedBorderColor = Color(0xFF393941), focusedTextColor = Color.White, unfocusedTextColor = Color.White))
                    Text("La reproducción se abre en YouTube. Las películas compradas o alquiladas se ven desde YouTube con la cuenta que las adquirió; esta app no las transfiere ni descarga.", color = Muted, fontSize = 12.sp)
                }
            },
            confirmButton = { TextButton(onClick = {
                val k = apiInput.trim()
                if (k.isBlank()) return@TextButton
                saveApiKey(k); apiKey = k; showSettings = false; message = "Clave guardada en este dispositivo. Hacé una búsqueda para probarla."
            }) { Text("Guardar", color = Gold) } },
            dismissButton = { if (apiKey.isNotBlank()) TextButton(onClick = { showSettings = false }) { Text("Cancelar") } },
            containerColor = Panel
        )
    }
}

private suspend fun searchYoutube(apiKey: String, query: String, live: Boolean): List<YoutubeVideo> = withContext(Dispatchers.IO) {
    val params = buildList {
        add("part" to "snippet"); add("type" to "video"); add("maxResults" to "25"); add("q" to query); add("key" to apiKey)
        add("regionCode" to "AR"); add("relevanceLanguage" to "es")
        if (live) add("eventType" to "live")
    }
    val queryString = params.joinToString("&") { (k,v) -> "${URLEncoder.encode(k, "UTF-8")}=${URLEncoder.encode(v, "UTF-8")}" }
    val connection = (URL("https://www.googleapis.com/youtube/v3/search?$queryString").openConnection() as HttpURLConnection).apply {
        connectTimeout = 15000; readTimeout = 20000; requestMethod = "GET"
    }
    try {
        val status = connection.responseCode
        val stream = if (status in 200..299) connection.inputStream else connection.errorStream
        val body = stream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        val json = JSONObject(body)
        if (status !in 200..299) {
            val apiError = json.optJSONObject("error")
            val reason = apiError?.optJSONArray("errors")?.optJSONObject(0)?.optString("reason").orEmpty()
            throw IllegalStateException(when(reason) {
                "quotaExceeded", "dailyLimitExceeded" -> "Se agotó la cuota diaria de búsqueda de YouTube."
                "keyInvalid" -> "La clave de YouTube API no es válida. Revisá la configuración."
                "keyExpired" -> "La clave de YouTube API venció."
                else -> apiError?.optString("message") ?: "Error de YouTube API ($status)."
            })
        }
        val items = json.optJSONArray("items") ?: return@withContext emptyList()
        buildList {
            for (i in 0 until items.length()) {
                val result = items.optJSONObject(i) ?: continue
                val id = result.optJSONObject("id")?.optString("videoId").orEmpty()
                if (id.isBlank()) continue
                val snippet = result.optJSONObject("snippet") ?: continue
                val thumbs = snippet.optJSONObject("thumbnails")
                val thumbnail = thumbs?.optJSONObject("medium")?.optString("url")?.takeIf { it.startsWith("https://") }
                    ?: thumbs?.optJSONObject("default")?.optString("url")?.takeIf { it.startsWith("https://") }
                add(YoutubeVideo(id, snippet.optString("title", "Video"), snippet.optString("channelTitle", "Canal de YouTube"), thumbnail, live, snippet.optString("description", "")))
            }
        }
    } finally { connection.disconnect() }
}

@Composable
private fun YoutubeRow(item: YoutubeVideo, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(15.dp)).background(Panel).clickable(onClick = onClick).padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(50.dp).clip(RoundedCornerShape(12.dp)).background(if (item.live) Color(0xFF352020) else Color(0xFF302916)), contentAlignment = Alignment.Center) {
            Icon(if (item.live) Icons.Default.LiveTv else Icons.Default.Movie, null, tint = if(item.live) Red else Gold, modifier = Modifier.size(25.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(item.title, color = Color.White, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(4.dp)); Text(item.channel, color = Muted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (item.description.isNotBlank()) Text(item.description, color = Muted, fontSize = 11.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        Spacer(Modifier.width(8.dp))
        if(item.live) Text("EN VIVO", color = Red, fontSize = 9.sp, fontWeight = FontWeight.Bold) else Icon(Icons.Default.PlayArrow, null, tint = Gold)
    }
}
