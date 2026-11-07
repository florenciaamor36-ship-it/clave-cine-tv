package ar.com.laclave.cinetv.data

import android.net.Uri

data class MediaItem(
    val id: String,
    val title: String,
    val url: String,
    val group: String,
    val logo: String?,
    val isLive: Boolean,
    val userAgent: String? = null,
    val referrer: String? = null,
)

object M3uParser {
    fun parse(text: String): List<MediaItem> {
        val result = mutableListOf<MediaItem>()
        var pending: Map<String, String> = emptyMap()
        var pendingTitle: String? = null
        text.lineSequence().forEach { raw ->
            val line = raw.trim().removePrefix("\uFEFF")
            when {
                line.startsWith("#EXTINF", ignoreCase = true) -> {
                    val comma = line.indexOf(',')
                    pending = parseAttributes(line.substringBeforeLast(',', line))
                    pendingTitle = if (comma >= 0) line.substring(comma + 1).trim().ifBlank { null }
                        else pending["tvg-name"]
                }
                line.startsWith("#EXTVLCOPT:", ignoreCase = true) -> {
                    val option = line.substringAfter(':')
                    val key = option.substringBefore('=').trim().lowercase()
                    val value = option.substringAfter('=', "").trim()
                    if (key == "http-user-agent" && value.isNotBlank()) pending = pending + ("user-agent" to value)
                    if ((key == "http-referrer" || key == "http-referer") && value.isNotBlank()) pending = pending + ("referrer" to value)
                }
                line.isNotBlank() && !line.startsWith('#') -> {
                    val uri = runCatching { Uri.parse(line) }.getOrNull()
                    if (uri != null && uri.scheme in setOf("http", "https")) {
                        val title = pendingTitle ?: pending["tvg-name"] ?: uri.lastPathSegment ?: "Contenido"
                        val group = pending["group-title"].orEmpty().ifBlank { "Sin categoría" }
                        val lower = listOf(group, pending["tvg-id"].orEmpty(), title).joinToString(" ").lowercase()
                        val live = lower.contains("live") || lower.contains("tv") || lower.contains("canal") || lower.contains("noticias") || lower.contains("deporte")
                        result += MediaItem(
                            id = "${result.size}_${line.hashCode()}", title = title, url = line, group = group,
                            logo = pending["tvg-logo"]?.takeIf { it.startsWith("http://") || it.startsWith("https://") },
                            isLive = live, userAgent = pending["user-agent"], referrer = pending["referrer"]
                        )
                    }
                    pending = emptyMap()
                    pendingTitle = null
                }
            }
        }
        return result
    }

    private fun parseAttributes(line: String): Map<String, String> {
        val attrs = mutableMapOf<String, String>()
        val pattern = Regex("([A-Za-z0-9_-]+)\\s*=\\s*(?:\"([^\"]*)\"|'([^']*)'|([^\\s,]+))")
        pattern.findAll(line).forEach { match ->
            val key = match.groupValues[1].lowercase()
            val value = listOf(match.groupValues[2], match.groupValues[3], match.groupValues[4]).firstOrNull { it.isNotEmpty() }.orEmpty()
            attrs[key] = value
        }
        return attrs
    }
}
