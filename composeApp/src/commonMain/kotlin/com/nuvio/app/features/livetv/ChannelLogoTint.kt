package com.nuvio.app.features.livetv

import androidx.compose.ui.graphics.Color
import coil3.request.SuccessResult

/** The logo's dominant saturated colour, used to tint the channel card; null when it has none (white logos). */
internal expect fun channelLogoTint(result: SuccessResult): Color?
