package com.nuvio.app.features.downloads

// iOS has no pre-download check yet: the download starts as before.
internal actual suspend fun probeDownloadSource(
    url: String,
    headers: Map<String, String>,
): DownloadSourceProbe = DownloadSourceProbe.Ok
