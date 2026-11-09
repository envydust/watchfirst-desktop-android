package com.nuvio.app.features.livetv

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.request.SuccessResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.nuvio.app.core.ui.nuvio

/** Live red, used only on the "AO VIVO" badge. */
internal val LiveBadgeRed = Color(0xFFE5173F)
internal val FavoriteGold = Color(0xFFFFC107)
private val ChannelCardShape = RoundedCornerShape(12.dp)
private val ChannelTileShape = RoundedCornerShape(10.dp)

/** Channel colours already read from their logos, so each logo is analysed once per session. */
private object ChannelTintCache {
    private val colors = mutableMapOf<String, Color>()
    fun get(key: String): Color? = colors[key]
    fun put(key: String, color: Color) {
        if (colors.size > 400) colors.clear()
        colors[key] = color
    }
}

/** "⚡ AO VIVO" pill with a drawn bolt, as on Disney+. */
@Composable
internal fun LiveBadge(modifier: Modifier = Modifier, compact: Boolean = false) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(5.dp))
            .background(LiveBadgeRed)
            .padding(start = 5.dp, end = 7.dp, top = 3.dp, bottom = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Rounded.Bolt,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(if (compact) 13.dp else 15.dp),
        )
        Spacer(Modifier.size(2.dp))
        Text(
            text = "AO VIVO",
            color = Color.White,
            fontSize = if (compact) 10.sp else 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.4.sp,
        )
    }
}

/** Channel subtitle: category and addon, the facts the live addons actually send. */
internal val LiveChannel.subtitle: String
    get() = listOf(category, addonName).filter { it.isNotBlank() }.distinct().joinToString(" · ")

/**
 * Background for a channel: its real artwork when the addon sends one, otherwise a field tinted with the
 * logo's own colour and the logo itself oversized and turned, bleeding off the corner.
 */
@Composable
private fun ChannelArt(
    channel: LiveChannel,
    watermark: Boolean,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.nuvio.colors
    val logoUrl = channel.imageUrl
    val artworkUrl = (channel.meta.landscapePoster ?: channel.meta.banner)?.takeIf { it.isNotBlank() && it != logoUrl }
    // Color.Unspecified in the cache means "analysed, no saturated colour" (white logos stay neutral).
    var tint by remember(logoUrl) { mutableStateOf(logoUrl?.let(ChannelTintCache::get)) }
    var logoResult by remember(logoUrl) { mutableStateOf<SuccessResult?>(null) }
    LaunchedEffect(logoResult) {
        val result = logoResult ?: return@LaunchedEffect
        if (tint != null || logoUrl == null) return@LaunchedEffect
        val extracted = withContext(Dispatchers.Default) { channelLogoTint(result) } ?: Color.Unspecified
        tint = extracted
        ChannelTintCache.put(logoUrl, extracted)
    }
    val base = tint?.takeIf { it != Color.Unspecified } ?: colors.surfaceCard

    BoxWithConstraints(modifier = modifier.background(colors.surface)) {
        if (artworkUrl != null) {
            AsyncImage(
                model = artworkUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.linearGradient(
                            0f to lerp(base, Color.Black, 0.12f),
                            0.55f to lerp(base, Color.Black, 0.48f),
                            1f to lerp(base, colors.background, 0.86f),
                        ),
                    ),
            )
            if (logoUrl != null && watermark) {
                val size = maxWidth * 1.15f
                AsyncImage(
                    model = logoUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = maxWidth * 0.32f, y = -maxHeight * 0.12f)
                        .requiredSize(size)
                        .rotate(-18f)
                        .graphicsLayer { alpha = 0.16f },
                )
            }
        }
        // Reads the logo once (hidden) to tint the card even when the logo is not drawn at full size.
        if (logoUrl != null && tint == null) {
            AsyncImage(
                model = logoUrl,
                contentDescription = null,
                modifier = Modifier.size(96.dp).graphicsLayer { alpha = 0f },
                onSuccess = { state -> logoResult = state.result },
            )
        }
    }
}

/** Disney-style channel card: tall, art-led, badge top left, logo and name bottom left. */
@Composable
internal fun LiveChannelCard(
    channel: LiveChannel,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    favorite: Boolean = false,
    onToggleFavorite: (() -> Unit)? = null,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val focused by interaction.collectIsFocusedAsState()
    val highlighted = pressed || focused
    val colors = MaterialTheme.nuvio.colors
    Box(
        modifier = modifier
            .aspectRatio(0.96f)
            .clip(ChannelCardShape)
            .border(
                width = if (highlighted) 2.dp else 1.dp,
                color = if (highlighted) Color.White else colors.borderSubtle,
                shape = ChannelCardShape,
            )
            .clickable(interactionSource = interaction, indication = LocalIndication.current, onClick = onClick),
    ) {
        ChannelArt(channel = channel, watermark = true, modifier = Modifier.fillMaxSize())
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to Color.Transparent,
                        0.42f to Color.Transparent,
                        1f to Color.Black.copy(alpha = 0.88f),
                    ),
                ),
        )
        LiveBadge(modifier = Modifier.align(Alignment.TopStart).padding(12.dp))
        if (onToggleFavorite != null) {
            IconButton(onClick = onToggleFavorite, modifier = Modifier.align(Alignment.TopEnd).padding(2.dp)) {
                Box(
                    modifier = Modifier.size(32.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.45f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = if (favorite) Icons.Rounded.Star else Icons.Rounded.StarBorder,
                        contentDescription = if (favorite) "Remover dos favoritos" else "Adicionar aos favoritos",
                        tint = if (favorite) FavoriteGold else Color.White,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(start = 14.dp, end = 14.dp, bottom = 14.dp),
        ) {
            channel.imageUrl?.let { logo ->
                AsyncImage(
                    model = logo,
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    alignment = Alignment.CenterStart,
                    modifier = Modifier.fillMaxWidth(0.62f).height(42.dp),
                )
                Spacer(Modifier.height(10.dp))
            }
            Text(
                text = channel.meta.name,
                color = Color.White,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = channel.subtitle,
                color = Color.White.copy(alpha = 0.74f),
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** Compact 16:9 tile for the "Destaques ao vivo" row in Busca: art with the badge, name and facts below. */
@Composable
internal fun LiveChannelTile(
    channel: LiveChannel,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val focused by interaction.collectIsFocusedAsState()
    val highlighted = pressed || focused
    val colors = MaterialTheme.nuvio.colors
    Column(
        modifier = modifier.clickable(interactionSource = interaction, indication = null, onClick = onClick),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .clip(ChannelTileShape)
                .border(
                    width = if (highlighted) 2.dp else 1.dp,
                    color = if (highlighted) Color.White else colors.borderSubtle,
                    shape = ChannelTileShape,
                ),
        ) {
            ChannelArt(channel = channel, watermark = false, modifier = Modifier.fillMaxSize())
            channel.imageUrl?.takeIf { channel.meta.landscapePoster == null && channel.meta.banner == null }?.let { logo ->
                AsyncImage(
                    model = logo,
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    // Sits above the badge row so the two never touch.
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(bottom = 26.dp)
                        .fillMaxWidth(0.52f)
                        .fillMaxHeight(0.36f),
                )
            }
            LiveBadge(compact = true, modifier = Modifier.align(Alignment.BottomStart).padding(8.dp))
        }
        Column {
            Text(
                text = channel.meta.name,
                color = colors.textPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = channel.subtitle,
                color = colors.textSecondary,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.widthIn(max = 260.dp),
            )
        }
    }
}
