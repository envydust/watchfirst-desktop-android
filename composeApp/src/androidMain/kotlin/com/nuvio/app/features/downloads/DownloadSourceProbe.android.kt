package com.nuvio.app.features.downloads

import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request

private const val PROBE_BYTES = 64 * 1024

private val probeHttpClient by lazy {
    downloadHttpClient.newBuilder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .callTimeout(25, TimeUnit.SECONDS)
        .build()
}

internal actual suspend fun probeDownloadSource(
    url: String,
    headers: Map<String, String>,
): DownloadSourceProbe = withContext(Dispatchers.IO) {
    val request = Request.Builder().url(url).apply {
        headers.forEach { (key, value) ->
            if (!key.equals("Range", true) && !key.equals("If-Range", true) && !key.equals("Accept-Encoding", true)) {
                header(key, value)
            }
        }
        header("Accept-Encoding", "identity")
        header("Range", "bytes=0-${PROBE_BYTES - 1}")
    }.build()
    try {
        probeHttpClient.newCall(request).execute().use { response ->
            if (response.code != 200 && response.code != 206) {
                return@withContext DownloadSourceProbe.Failed("o servidor respondeu com erro ${response.code}")
            }
            val contentType = response.header("Content-Type").orEmpty().lowercase()
            val body = response.body ?: return@withContext DownloadSourceProbe.Failed("o servidor não enviou o vídeo")
            val head = ByteArray(PROBE_BYTES)
            var read = 0
            body.byteStream().use { input ->
                while (read < head.size) {
                    val n = input.read(head, read, head.size - read)
                    if (n <= 0) break
                    read += n
                }
            }
            val bytes = head.copyOf(read)
            when {
                read == 0 -> DownloadSourceProbe.Failed("o servidor não enviou o vídeo")
                looksLikeVideo(bytes) -> DownloadSourceProbe.Ok
                contentType.startsWith("text/") || contentType.contains("json") || looksLikeTextPage(bytes) ->
                    DownloadSourceProbe.Failed("o servidor mandou uma página em vez do vídeo")
                // Unknown binary format: do not block it.
                else -> DownloadSourceProbe.Ok
            }
        }
    } catch (cancel: CancellationException) {
        throw cancel
    } catch (_: Exception) {
        DownloadSourceProbe.Failed("não foi possível conectar")
    }
}
