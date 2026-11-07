package ar.com.laclave.cinetv

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.MediaItem as ExoMediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import ar.com.laclave.cinetv.data.M3uParser
import ar.com.laclave.cinetv.data.MediaItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

private val Ink = Color(0xFF0B0B0E)
private val Panel = Color(0xFF19191F)
private val Gold = Color(0xFFE5B847)
private val Soft = Color(0xFFAAAAB3)
private val LiveRed = Color(0xFFE05A52)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val preferences = getSharedPreferences("clave_cine_tv", MODE_PRIVATE)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme(primary = Gold, background = Ink, surface = Panel, onPrimary = Ink, onBackground = Color.White, onSurface = Color.White)) {
                ClaveCineApp(
                    loadSaved = { preferences.getString("playlist_text", null) },
                    saveList = { text -> preferences.edit().putString("playlist_text", text).apply() }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ClaveCineApp(loadSaved: () -> String?, saveList: (String) -> Unit) {
    val context = LocalContext.current
    var allItems by remember { mutableStateOf(emptyList<MediaItem>()) }
    var title by remember { mutableStateOf("Tu contenido") }
    var query by remember { mutableStateOf("") }
    var selectedTab by remember { mutableIntStateOf(0) }
    var notice by remember { mutableStateOf("Importá una lista M3U/M3U8 propia o autorizada para empezar.") }
    var busy by remember { mutableStateOf(false) }
    var showUrl by remember { mutableStateOf(false) }
    var sourceUrl by remember { mutableStateOf("") }
    var playing by remember { mutableStateOf<MediaItem?>(null) }
    var confirmImport by remember { mutableStateOf(false) }
    var pendingText by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        val saved = withContext(Dispatchers.IO) { loadSaved() }
        if (!saved.isNullOrBlank()) {
            allItems = M3uParser.parse(saved)
            title = "Mi lista guardada"
            if (allItems.isNotEmpty()) notice = "Lista cargada: ${allItems.size} elementos."
        }
    }

    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) {
            busy = true
            notice = "Leyendo lista…"
            (context as? ComponentActivity)?.lifecycleScope?.launch {
                val text = withContext(Dispatchers.IO) {
                    runCatching { context.contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use { it.readText() } }.getOrNull()
                }
                if (text.isNullOrBlank()) {
                    notice = "No pude leer el archivo. Probá una lista .m3u o .m3u8 válida."
                } else {
                    pendingText = text
                    confirmImport = true
                }
                busy = false
            }
        }
    }

    val visibleItems = allItems.filter { item ->
        val matchesTab = when (selectedTab) {
            1 -> item.isLive
            2 -> !item.isLive
            else -> true
        }
        matchesTab && (query.isBlank() || item.title.contains(query, true) || item.group.contains(query, true))
    }

    Scaffold(
        containerColor = Ink,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("LA CLAVE", color = Gold, fontWeight = FontWeight.Black, letterSpacing = 2.sp, fontSize = 18.sp)
                        Text("CINE · TV", color = Soft, fontSize = 11.sp, letterSpacing = 1.4.sp)
                    }
                },
                actions = {
                    IconButton(onClick = { showUrl = true }) { Icon(Icons.Default.Link, "Agregar lista por enlace", tint = Gold) }
                    IconButton(onClick = { filePicker.launch(arrayOf("*/*")) }) { Icon(Icons.Default.PlaylistAdd, "Importar lista", tint = Color.White) }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Ink)
            )
        },
        bottomBar = {
            NavigationBar(containerColor = Panel, contentColor = Gold) {
                listOf("Inicio", "En vivo", "Películas").forEachIndexed { index, label ->
                    NavigationBarItem(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        icon = { Icon(when(index) { 1 -> Icons.Default.LiveTv; 2 -> Icons.Default.Movie; else -> Icons.Default.Home }, contentDescription = label) },
                        label = { Text(label) },
                        colors = NavigationBarItemDefaults.colors(selectedIconColor = Gold, selectedTextColor = Gold, indicatorColor = Color(0xFF332A18), unselectedIconColor = Soft, unselectedTextColor = Soft)
                    )
                }
            }
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Text("Películas y canales, en un solo lugar", Modifier.padding(start = 20.dp, end = 20.dp, top = 12.dp), color = Color.White, fontSize = 21.sp, fontWeight = FontWeight.Bold)
            Text("Reproducí contenido de tus listas autorizadas", Modifier.padding(horizontal = 20.dp, vertical = 4.dp), color = Soft, fontSize = 13.sp)
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = query, onValueChange = { query = it }, modifier = Modifier.weight(1f), singleLine = true,
                    placeholder = { Text("Buscar canal o película", color = Soft) }, leadingIcon = { Icon(Icons.Default.Search, null, tint = Soft) },
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Gold, unfocusedBorderColor = Color(0xFF393941), focusedTextColor = Color.White, unfocusedTextColor = Color.White, cursorColor = Gold)
                )
                FilledTonalIconButton(onClick = { filePicker.launch(arrayOf("*/*")) }, modifier = Modifier.size(56.dp)) { Icon(Icons.Default.Add, "Importar lista", tint = Gold) }
            }
            Row(Modifier.padding(horizontal = 20.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(if (allItems.isEmpty()) notice else "$title · ${visibleItems.size} elementos", modifier = Modifier.weight(1f), color = Soft, fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                if (busy) CircularProgressIndicator(Modifier.size(22.dp), color = Gold, strokeWidth = 2.dp)
            }
            Spacer(Modifier.height(8.dp))
            if (allItems.isEmpty()) {
                EmptyState(onImport = { filePicker.launch(arrayOf("*/*")) }, onUrl = { showUrl = true })
            } else if (visibleItems.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("No encontré contenido con ese filtro.", color = Soft) }
            } else {
                LazyColumn(contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 20.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    items(visibleItems, key = { it.id }) { item ->
                        MediaRow(item) { playing = item }
                    }
                }
            }
        }
    }

    if (showUrl) {
        AlertDialog(
            onDismissRequest = { showUrl = false },
            title = { Text("Agregar lista M3U") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Pegá un enlace directo a una lista que tengas permiso de usar.", color = Soft, fontSize = 13.sp)
                    OutlinedTextField(value = sourceUrl, onValueChange = { sourceUrl = it }, singleLine = true, label = { Text("https://…/lista.m3u8") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri))
                }
            },
            confirmButton = { TextButton(onClick = {
                val link = sourceUrl.trim()
                if (!link.startsWith("https://", true)) { notice = "Por seguridad, la lista debe usar HTTPS."; return@TextButton }
                showUrl = false; busy = true; notice = "Cargando lista…"
                (context as? ComponentActivity)?.lifecycleScope?.launch {
                    val text = withContext(Dispatchers.IO) { fetchText(link) }
                    busy = false
                    if (text == null) notice = "No se pudo cargar la lista. Revisá el enlace y el acceso."
                    else { pendingText = text; confirmImport = true; title = "Lista desde enlace" }
                }
            }) { Text("Cargar", color = Gold) } },
            dismissButton = { TextButton(onClick = { showUrl = false }) { Text("Cancelar") } },
            containerColor = Panel
        )
    }

    if (confirmImport) {
        val parsed = remember(pendingText) { M3uParser.parse(pendingText) }
        AlertDialog(
            onDismissRequest = { confirmImport = false },
            title = { Text("Importar lista") },
            text = { Text(if (parsed.isEmpty()) "No encontré enlaces reproducibles en esta lista." else "Encontré ${parsed.size} elementos. Se guardarán solo en este dispositivo.", color = Soft) },
            confirmButton = { TextButton(enabled = parsed.isNotEmpty(), onClick = {
                allItems = parsed
                saveList(pendingText)
                title = "Mi lista"
                notice = "Lista importada: ${parsed.size} elementos."
                confirmImport = false
            }) { Text("Guardar en este dispositivo", color = Gold) } },
            dismissButton = { TextButton(onClick = { confirmImport = false }) { Text("Cancelar") } },
            containerColor = Panel
        )
    }

    playing?.let { item ->
        PlayerDialog(item = item, onClose = { playing = null })
    }
}

private suspend fun fetchText(link: String): String? = withContext(Dispatchers.IO) {
    runCatching {
        val connection = URL(link).openConnection() as HttpURLConnection
        connection.connectTimeout = 15000
        connection.readTimeout = 20000
        connection.setRequestProperty("User-Agent", "ClaveCineTV/0.1")
        connection.instanceFollowRedirects = true
        connection.connect()
        require(connection.url.protocol.equals("https", ignoreCase = true)) { "Redirected to non-HTTPS URL" }
        require(connection.contentLengthLong <= 5L * 1024 * 1024 || connection.contentLengthLong < 0L) { "Playlist too large" }
        connection.inputStream.bufferedReader(Charsets.UTF_8).use { reader ->
            val text = reader.readText()
            require(text.length <= 5 * 1024 * 1024) { "Playlist too large" }
            text
        }.also { connection.disconnect() }
    }.getOrNull()
}

@Composable
private fun EmptyState(onImport: () -> Unit, onUrl: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Box(Modifier.size(94.dp).clip(RoundedCornerShape(28.dp)).background(Color(0xFF282115)), contentAlignment = Alignment.Center) {
            Icon(Icons.Default.LiveTv, null, tint = Gold, modifier = Modifier.size(48.dp))
        }
        Spacer(Modifier.height(20.dp))
        Text("Tu cine empieza acá", color = Color.White, fontSize = 23.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text("Importá tu archivo o enlace M3U/M3U8. Solo agregá contenido propio o con autorización.", color = Soft, fontSize = 14.sp, lineHeight = 21.sp)
        Spacer(Modifier.height(22.dp))
        Button(onClick = onImport, colors = ButtonDefaults.buttonColors(containerColor = Gold, contentColor = Ink), modifier = Modifier.fillMaxWidth()) { Icon(Icons.Default.FolderOpen, null); Spacer(Modifier.width(8.dp)); Text("Elegir archivo M3U") }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = onUrl, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.outlinedButtonColors(contentColor = Gold)) { Icon(Icons.Default.Link, null); Spacer(Modifier.width(8.dp)); Text("Pegar enlace de lista") }
        Spacer(Modifier.height(16.dp))
        Text("Las listas y sus enlaces se guardan localmente en este teléfono.", color = Soft, fontSize = 11.sp)
    }
}

@Composable
private fun MediaRow(item: MediaItem, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(15.dp)).background(Panel).clickable(onClick = onClick).padding(13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(54.dp).clip(RoundedCornerShape(13.dp)).background(if (item.isLive) Color(0xFF352020) else Color(0xFF302916)), contentAlignment = Alignment.Center) {
            Icon(if (item.isLive) Icons.Default.LiveTv else Icons.Default.Movie, null, tint = if (item.isLive) LiveRed else Gold, modifier = Modifier.size(27.dp))
        }
        Spacer(Modifier.width(13.dp))
        Column(Modifier.weight(1f)) {
            Text(item.title, color = Color.White, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(4.dp))
            Text(item.group, color = Soft, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Spacer(Modifier.width(8.dp))
        if (item.isLive) Text("EN VIVO", color = LiveRed, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.clip(CircleShape).background(Color(0xFF3A2223)).padding(horizontal = 9.dp, vertical = 5.dp))
        else Icon(Icons.Default.PlayArrow, null, tint = Gold)
    }
}

@Composable
private fun PlayerDialog(item: MediaItem, onClose: () -> Unit) {
    val context = LocalContext.current
    val player = remember(item.url) {
        val dataSource = DefaultHttpDataSource.Factory().setUserAgent(item.userAgent ?: "ClaveCineTV/0.1").apply {
            val headers = mutableMapOf<String, String>()
            item.referrer?.let { headers["Referer"] = it }
            if (headers.isNotEmpty()) setDefaultRequestProperties(headers)
        }
        ExoPlayer.Builder(context).setMediaSourceFactory(androidx.media3.exoplayer.source.DefaultMediaSourceFactory(dataSource)).build().apply {
            setMediaItem(ExoMediaItem.Builder().setUri(item.url).setMediaMetadata(MediaMetadata.Builder().setTitle(item.title).build()).build())
            prepare(); playWhenReady = true
        }
    }
    DisposableEffect(player) { onDispose { player.release() } }
    AlertDialog(
        onDismissRequest = onClose,
        title = { Text(item.title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        text = {
            Column {
                AndroidView(factory = { ctx -> PlayerView(ctx).apply { this.player = player; useController = true; keepScreenOn = true } }, modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f))
                Spacer(Modifier.height(10.dp))
                Text(if (item.isLive) "Canal en vivo" else "Película / video", color = Soft, fontSize = 12.sp)
            }
        },
        confirmButton = { TextButton(onClick = onClose) { Text("Cerrar", color = Gold) } },
        containerColor = Panel
    )
}
