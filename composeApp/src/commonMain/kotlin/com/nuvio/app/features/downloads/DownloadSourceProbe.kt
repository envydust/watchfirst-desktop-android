package com.nuvio.app.features.downloads

import com.nuvio.app.core.ui.NuvioToastController
import com.nuvio.app.features.streams.StreamItem

// WATCHFIRST (member feedback): test the chosen server before a download starts, so a source that does
// not work is noticed right away instead of after the episode has "downloaded".

internal sealed interface DownloadSourceProbe {
    data object Ok : DownloadSourceProbe
    data class Failed(val reason: String) : DownloadSourceProbe
}

/**
 * Asks the server for the first bytes of the file and checks that they look like a video.
 * Platforms without a check return [DownloadSourceProbe.Ok].
 */
internal expect suspend fun probeDownloadSource(
    url: String,
    headers: Map<String, String>,
): DownloadSourceProbe

/**
 * Tests the stream before downloading. Returns the message to show when the server does not work, or null
 * when the download can start (also when the link is not a direct file: the download step explains that).
 */
internal suspend fun testDownloadSource(stream: StreamItem): String? {
    val url = stream.playableDirectUrl?.trim()?.takeIf { it.isNotBlank() } ?: return null
    if (!url.isSupportedDownloadUrl()) return null
    NuvioToastController.show("Testando o servidor antes de baixar…", durationMillis = 6000L)
    return when (val result = probeDownloadSource(url, stream.behaviorHints.proxyHeaders?.request.orEmpty())) {
        DownloadSourceProbe.Ok -> null
        is DownloadSourceProbe.Failed ->
            "Este servidor não está funcionando agora (${result.reason}). Escolha outro servidor para baixar."
    }
}

/** Container signatures of the video files addons serve. */
internal fun looksLikeVideo(head: ByteArray): Boolean {
    fun at(offset: Int, vararg bytes: Int): Boolean =
        head.size >= offset + bytes.size && bytes.indices.all { (head[offset + it].toInt() and 0xFF) == bytes[it] }
    return at(4, 0x66, 0x74, 0x79, 0x70) || // MP4 / MOV / M4V: "ftyp"
        at(0, 0x1A, 0x45, 0xDF, 0xA3) || // Matroska / WebM
        (at(0, 0x47) && at(188, 0x47)) || // MPEG-TS
        at(0, 0x52, 0x49, 0x46, 0x46) || // AVI: "RIFF"
        at(0, 0x46, 0x4C, 0x56) || // FLV
        at(0, 0x00, 0x00, 0x01, 0xBA) // MPEG-PS
}

/** An HTML page or a JSON error where the video should be. */
internal fun looksLikeTextPage(head: ByteArray): Boolean {
    val first = head.map { it.toInt() and 0xFF }.firstOrNull { it !in listOf(0x20, 0x09, 0x0A, 0x0D, 0xEF, 0xBB, 0xBF) }
        ?: return false
    return first == '<'.code || first == '{'.code || first == '['.code
}
