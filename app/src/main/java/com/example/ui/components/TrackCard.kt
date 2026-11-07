package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Track
import com.example.ui.theme.OutfitFontFamily
import com.example.ui.theme.RajdhaniFontFamily
import com.example.ui.theme.SportCardBorder
import com.example.ui.theme.SportCardSurface
import com.example.ui.theme.SportCardSurfaceVariant
import com.example.ui.theme.SportCyan
import com.example.ui.theme.SportCyanDim
import com.example.ui.theme.SportLime
import com.example.ui.theme.SportOrange
import com.example.ui.theme.SportOrangeDim
import com.example.ui.theme.SportRed
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextWhite

@Composable
fun TrackCard(
    track: Track,
    orderNumber: Int,
    isShuffleActive: Boolean,
    isCurrentlyPlaying: Boolean,
    onTrackClick: () -> Unit,
    isFavorite: Boolean = false,
    onFavoriteToggle: (() -> Unit)? = null,
    onAddToPlaylistClick: (() -> Unit)? = null,
    onDeleteClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val borderColor = if (isCurrentlyPlaying) SportOrange else Color.Transparent
    val cardBg = if (isCurrentlyPlaying) SportCardSurfaceVariant else SportCardSurface

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = cardBg,
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(
                width = if (isCurrentlyPlaying) 1.5.dp else 0.5.dp,
                color = if (isCurrentlyPlaying) SportOrange else SportCardBorder,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable { onTrackClick() }
            .testTag("track_card_${track.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // ORDER NUMBER (Always visible regardless of whether album art exists)
            Box(
                modifier = Modifier
                    .width(26.dp)
                    .testTag("track_order_${track.id}"),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isShuffleActive) "#$orderNumber" else "%02d".format(orderNumber),
                    fontFamily = RajdhaniFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = if (isShuffleActive) 13.sp else 12.sp,
                    color = when {
                        isCurrentlyPlaying -> SportOrange
                        isShuffleActive -> SportOrange
                        else -> TextMuted
                    }
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // ALBUM ART / ICON THUMBNAIL
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        when {
                            isCurrentlyPlaying -> SportOrangeDim
                            else -> SportCardBorder
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (!track.albumArtUri.isNullOrBlank()) {
                    coil.compose.AsyncImage(
                        model = track.albumArtUri,
                        contentDescription = null,
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(8.dp))
                    )
                    if (isCurrentlyPlaying) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.55f)),
                            contentAlignment = Alignment.Center
                        ) {
                            MiniWaveAnimation()
                        }
                    }
                } else if (isCurrentlyPlaying) {
                    MiniWaveAnimation()
                } else {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = null,
                        tint = TextMuted,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Track Details
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = track.title,
                    fontFamily = OutfitFontFamily,
                    fontWeight = if (isCurrentlyPlaying) FontWeight.Bold else FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = if (isCurrentlyPlaying) SportOrange else TextWhite,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(2.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = track.artist,
                        fontFamily = OutfitFontFamily,
                        fontSize = 12.sp,
                        color = TextMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )

                    Spacer(modifier = Modifier.width(6.dp))

                    // Hi-Res Audio Spec Badge
                    Text(
                        text = track.audioSpecsBadge,
                        fontFamily = RajdhaniFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 9.sp,
                        color = if (track.isHiRes) SportCyan else SportLime,
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(SportCyanDim)
                            .padding(horizontal = 5.dp, vertical = 1.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Duration & Delete Button
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = track.durationFormatted,
                    fontFamily = RajdhaniFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    color = if (isCurrentlyPlaying) SportOrange else TextMuted
                )

                // Love / Favorite button for one-tap favorite playlist management
                if (onFavoriteToggle != null) {
                    Spacer(modifier = Modifier.width(2.dp))
                    IconButton(
                        onClick = onFavoriteToggle,
                        modifier = Modifier
                            .size(34.dp)
                            .testTag("fav_btn_${track.id}")
                    ) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                            contentDescription = if (isFavorite) "Hapus dari Favorit" else "Tambah ke Favorit",
                            tint = if (isFavorite) SportRed else TextMuted,
                            modifier = Modifier.size(19.dp)
                        )
                    }
                }

                if (onAddToPlaylistClick != null) {
                    Spacer(modifier = Modifier.width(2.dp))
                    IconButton(
                        onClick = onAddToPlaylistClick,
                        modifier = Modifier
                            .size(34.dp)
                            .testTag("add_to_playlist_${track.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlaylistAdd,
                            contentDescription = "Tambah ke Playlist",
                            tint = SportCyan.copy(alpha = 0.8f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                if (onDeleteClick != null) {
                    Spacer(modifier = Modifier.width(2.dp))
                    IconButton(
                        onClick = onDeleteClick,
                        modifier = Modifier
                            .size(34.dp)
                            .testTag("delete_track_${track.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Hapus Lagu",
                            tint = TextMuted.copy(alpha = 0.6f),
                            modifier = Modifier.size(17.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MiniWaveAnimation() {
    val infiniteTransition = rememberInfiniteTransition(label = "wave_anim")
    val bar1 by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bar1"
    )
    val bar2 by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 0.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(550, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bar2"
    )
    val bar3 by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(350, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bar3"
    )

    Row(
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.height(16.dp)
    ) {
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(16.dp * bar1)
                .clip(RoundedCornerShape(1.dp))
                .background(Color.Black)
        )
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(16.dp * bar2)
                .clip(RoundedCornerShape(1.dp))
                .background(Color.Black)
        )
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(16.dp * bar3)
                .clip(RoundedCornerShape(1.dp))
                .background(Color.Black)
        )
    }
}
