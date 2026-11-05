package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Hearing
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import com.example.data.model.LyricLine
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextWhite
import com.example.ui.viewmodel.PlaybackRepeatMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FullPlayerSheet(
    track: Track?,
    isPlaying: Boolean,
    currentPositionMs: Long,
    durationMs: Long,
    orderBadge: Int,
    totalQueueCount: Int,
    isShuffleActive: Boolean,
    repeatMode: PlaybackRepeatMode,
    currentDecibels: Float,
    isSleepTimerActive: Boolean,
    sleepTimerRemainingSeconds: Long,
    isWaitingForTrackEndToSleep: Boolean = false,
    loudnessNormalizationActive: Boolean = true,
    appliedGainDb: Float = 0.0f,
    currentLyrics: List<LyricLine> = emptyList(),
    activeLyricIndex: Int = -1,
    onTogglePlayPause: () -> Unit,
    onPlayNext: () -> Unit,
    onPlayPrevious: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onToggleShuffle: () -> Unit,
    onReshuffle: () -> Unit,
    onCycleRepeat: () -> Unit,
    onOpenSleepTimer: () -> Unit,
    onOpenEqualizer: () -> Unit,
    onOpenLyrics: () -> Unit,
    onImportLyrics: () -> Unit = {},
    onPasteLyrics: () -> Unit = {},
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (track == null) return

    var isDraggingSlider by remember { mutableStateOf(false) }
    var dragPosition by remember { mutableFloatStateOf(0f) }

    val currentMs = if (isDraggingSlider) {
        (dragPosition * durationMs).toLong()
    } else currentPositionMs

    val progress = if (durationMs > 0) {
        (currentPositionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
    } else 0f

    // Vinyl rotation animation
    val infiniteTransition = rememberInfiniteTransition(label = "disc_spin")
    val angle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "angle"
    )
    val discRotation = if (isPlaying) angle else 0f

    Surface(
        color = MaterialTheme.colorScheme.background,
        modifier = modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onClose,
                    modifier = Modifier.testTag("full_player_close")
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "Minimize Player",
                        tint = TextWhite,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "SEDANG MEMUTAR",
                        fontFamily = RajdhaniFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = SportOrange,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = if (isShuffleActive) "Urutan Acak #$orderBadge dari $totalQueueCount" else "Urutan Normal #$orderBadge",
                        fontFamily = RajdhaniFontFamily,
                        fontSize = 11.sp,
                        color = SportCyan
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Sleep timer trigger button with Live High-Contrast Capsule Badge
                    if (isSleepTimerActive || isWaitingForTrackEndToSleep) {
                        val hours = sleepTimerRemainingSeconds / 3600
                        val mins = (sleepTimerRemainingSeconds % 3600) / 60
                        val secs = sleepTimerRemainingSeconds % 60
                        val badgeText = if (isWaitingForTrackEndToSleep) {
                            "Lagu Akhir"
                        } else if (hours > 0) {
                            "%d:%02d:%02d".format(hours, mins, secs)
                        } else {
                            "%02d:%02d".format(mins, secs)
                        }
                        Surface(
                            onClick = onOpenSleepTimer,
                            shape = RoundedCornerShape(20.dp),
                            color = SportOrange,
                            contentColor = Color.Black,
                            shadowElevation = 3.dp,
                            modifier = Modifier
                                .padding(end = 4.dp)
                                .testTag("full_player_sleep_btn")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Bedtime,
                                    contentDescription = "Sleep Timer Active",
                                    tint = Color.Black,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = badgeText,
                                    fontFamily = RajdhaniFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = Color.Black
                                )
                            }
                        }
                    } else {
                        IconButton(
                            onClick = onOpenSleepTimer,
                            modifier = Modifier.testTag("full_player_sleep_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Bedtime,
                                contentDescription = "Sleep Timer",
                                tint = TextMuted
                            )
                        }
                    }

                    // Equalizer trigger
                    IconButton(
                        onClick = onOpenEqualizer,
                        modifier = Modifier.testTag("full_player_eq_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Equalizer,
                            contentDescription = "Equalizer",
                            tint = TextWhite
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Album Art: Real Cover Image if available, otherwise stylish Vinyl Disc
            if (!track.albumArtUri.isNullOrBlank()) {
                Box(
                    modifier = Modifier
                        .size(220.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(SportCardSurface)
                        .border(1.5.dp, SportCardBorder, RoundedCornerShape(20.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = track.albumArtUri,
                        contentDescription = "Cover Album ${track.title}",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(20.dp))
                    )
                }
            } else {
                // Sporty Vinyl Disc Album Art
                Box(
                    modifier = Modifier
                        .size(220.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                listOf(SportOrange.copy(alpha = 0.25f), Color.Transparent)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    // Vinyl outer groove
                    Box(
                        modifier = Modifier
                            .size(190.dp)
                            .rotate(discRotation)
                            .clip(CircleShape)
                            .background(
                                Brush.sweepGradient(
                                    listOf(
                                        Color(0xFF161B22),
                                        Color(0xFF212833),
                                        Color(0xFF161B22),
                                        Color(0xFF2C3545),
                                        Color(0xFF161B22)
                                    )
                                )
                            )
                            .border(2.dp, SportCardBorder, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        // Inner Center Label with Sporty Emblem
                        Box(
                            modifier = Modifier
                                .size(70.dp)
                                .clip(CircleShape)
                                .background(SportOrange)
                                .border(3.dp, Color.Black, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.GraphicEq,
                                contentDescription = null,
                                tint = Color.Black,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Audio Specs HUD Row (Hi-Res specs & Real-time decibels - with clean dot separators, no vertical bars)
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = SportCardSurface,
                border = androidx.compose.foundation.BorderStroke(1.dp, SportCardBorder)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = track.audioSpecsBadge,
                        fontFamily = RajdhaniFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = SportCyan
                    )

                    Text(
                        text = "•",
                        fontSize = 9.sp,
                        color = TextMuted.copy(alpha = 0.5f)
                    )

                    Text(
                        text = "${track.bitrateKbps} kbps",
                        fontFamily = RajdhaniFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 11.sp,
                        color = TextWhite
                    )

                    Text(
                        text = "•",
                        fontSize = 9.sp,
                        color = TextMuted.copy(alpha = 0.5f)
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Hearing,
                            contentDescription = null,
                            tint = if (currentDecibels <= -10f) SportLime else SportOrange,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "%.1f dB".format(currentDecibels),
                            fontFamily = RajdhaniFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = if (currentDecibels <= -10f) SportLime else SportOrange
                        )
                    }

                    if (loudnessNormalizationActive) {
                        Text(
                            text = "•",
                            fontSize = 9.sp,
                            color = TextMuted.copy(alpha = 0.5f)
                        )
                        Text(
                            text = "AVL %+.1f dB".format(appliedGainDb),
                            fontFamily = RajdhaniFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            color = SportLime,
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(SportLime.copy(alpha = 0.15f))
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Track Title & Artist
            Text(
                text = track.title,
                fontFamily = OutfitFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = TextWhite,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "${track.artist} — ${track.album}",
                fontFamily = OutfitFontFamily,
                fontSize = 13.sp,
                color = TextMuted,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(14.dp))

            // JENDELA KECIL LIRIK (Mini Lyrics Window on Main Player)
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = SportCardSurface,
                border = androidx.compose.foundation.BorderStroke(1.dp, SportCardBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .clickable { onOpenLyrics() }
                    .testTag("mini_lyrics_window")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.FormatQuote,
                                contentDescription = null,
                                tint = SportOrange,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "LIRIK BERJALAN",
                                fontFamily = RajdhaniFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = SportOrange,
                                letterSpacing = 1.sp
                            )
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(SportCyanDim)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "Layar Penuh",
                                fontFamily = RajdhaniFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                                color = SportCyan
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Icon(
                                imageVector = Icons.Default.OpenInNew,
                                contentDescription = null,
                                tint = SportCyan,
                                modifier = Modifier.size(10.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    if (currentLyrics.isNotEmpty()) {
                        val activeLine = currentLyrics.getOrNull(activeLyricIndex)
                        val nextLine = currentLyrics.getOrNull(activeLyricIndex + 1)
                        val prevLine = if (activeLyricIndex > 0) currentLyrics.getOrNull(activeLyricIndex - 1) else null

                        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            if (prevLine != null) {
                                Text(
                                    text = prevLine.text,
                                    fontFamily = OutfitFontFamily,
                                    fontSize = 11.sp,
                                    color = TextMuted.copy(alpha = 0.45f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Text(
                                text = activeLine?.text ?: "•••",
                                fontFamily = OutfitFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                lineHeight = 20.sp,
                                color = SportOrange,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (nextLine != null) {
                                Text(
                                    text = nextLine.text,
                                    fontFamily = OutfitFontFamily,
                                    fontSize = 12.sp,
                                    color = TextMuted,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    } else {
                        Column {
                            Text(
                                text = "Lirik belum terdeteksi untuk lagu ini.",
                                fontFamily = OutfitFontFamily,
                                fontSize = 11.sp,
                                color = TextMuted
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = SportOrangeDim,
                                    modifier = Modifier
                                        .clickable { onImportLyrics() }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Add,
                                            contentDescription = null,
                                            tint = SportOrange,
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "Pilih File .LRC",
                                            fontFamily = RajdhaniFontFamily,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 10.sp,
                                            color = SportOrange
                                        )
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = SportCyanDim,
                                    modifier = Modifier
                                        .clickable { onPasteLyrics() }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "✍️ Tempel Teks",
                                            fontFamily = RajdhaniFontFamily,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 10.sp,
                                            color = SportCyan
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // ORDERED SHUFFLE STATUS CARD (Crucial User Requirement)
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = SportCardSurfaceVariant),
                border = androidx.compose.foundation.BorderStroke(1.dp, SportCardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isShuffleActive) SportOrange else SportCardBorder),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (isShuffleActive) "#$orderBadge" else "%02d".format(orderBadge),
                                fontFamily = RajdhaniFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = if (isShuffleActive) Color.Black else TextMuted
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Column {
                            Text(
                                text = if (isShuffleActive) "SEDANG MEMUTAR URUTAN ACAK" else "URUTAN NORMAL DAFTAR",
                                fontFamily = RajdhaniFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = if (isShuffleActive) SportOrange else TextMuted
                            )
                            Text(
                                text = "Lagu ke #$orderBadge dari total $totalQueueCount lagu",
                                fontFamily = OutfitFontFamily,
                                fontSize = 12.sp,
                                color = TextWhite
                            )
                        }
                    }

                    if (isShuffleActive) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(SportOrangeDim)
                                .clickable { onReshuffle() }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                .testTag("reshuffle_full_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Acak Ulang",
                                tint = SportOrange,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Acak Ulang",
                                fontFamily = RajdhaniFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = SportOrange
                            )
                        }
                    }
                }
            }

            if (isWaitingForTrackEndToSleep) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = SportOrange.copy(alpha = 0.15f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, SportOrange),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bedtime,
                            contentDescription = null,
                            tint = SportOrange,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Timer habis: Menunggu lagu ini selesai baru mati",
                            fontFamily = OutfitFontFamily,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = SportOrange
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Scrubber Slider - Sleek round thumb without vertical ticks or lines
            Slider(
                value = if (isDraggingSlider) dragPosition else progress,
                onValueChange = {
                    isDraggingSlider = true
                    dragPosition = it
                },
                onValueChangeFinished = {
                    isDraggingSlider = false
                    val targetMs = (dragPosition * durationMs).toLong()
                    onSeekTo(targetMs)
                },
                thumb = {
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .clip(CircleShape)
                            .background(SportOrange)
                            .border(2.dp, Color.White, CircleShape)
                    )
                },
                colors = SliderDefaults.colors(
                    thumbColor = SportOrange,
                    activeTrackColor = SportOrange,
                    inactiveTrackColor = SportCardBorder,
                    activeTickColor = Color.Transparent,
                    inactiveTickColor = Color.Transparent
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("full_player_scrubber")
            )

            // Timestamps
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val elapsedSec = currentMs / 1000
                Text(
                    text = "%d:%02d".format(elapsedSec / 60, elapsedSec % 60),
                    fontFamily = RajdhaniFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp,
                    color = TextMuted
                )

                Text(
                    text = track.durationFormatted,
                    fontFamily = RajdhaniFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp,
                    color = TextMuted
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Playback Controls Row (Shuffle, Prev, Play/Pause, Next, Repeat)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Shuffle button
                IconButton(
                    onClick = onToggleShuffle,
                    modifier = Modifier.size(48.dp).testTag("full_player_shuffle_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Shuffle,
                        contentDescription = "Toggle Shuffle",
                        tint = if (isShuffleActive) SportOrange else TextMuted,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Previous button
                IconButton(
                    onClick = onPlayPrevious,
                    modifier = Modifier.size(48.dp).testTag("full_player_prev_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipPrevious,
                        contentDescription = "Previous Track",
                        tint = TextWhite,
                        modifier = Modifier.size(32.dp)
                    )
                }

                // Giant Play/Pause FAB
                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .clip(CircleShape)
                        .background(SportOrange)
                        .clickable { onTogglePlayPause() }
                        .testTag("full_player_play_pause_btn"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        tint = Color.Black,
                        modifier = Modifier.size(36.dp)
                    )
                }

                // Next button
                IconButton(
                    onClick = onPlayNext,
                    modifier = Modifier.size(48.dp).testTag("full_player_next_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipNext,
                        contentDescription = "Next Track",
                        tint = TextWhite,
                        modifier = Modifier.size(32.dp)
                    )
                }

                // Repeat Mode button
                IconButton(
                    onClick = onCycleRepeat,
                    modifier = Modifier.size(48.dp).testTag("full_player_repeat_btn")
                ) {
                    Icon(
                        imageVector = if (repeatMode == PlaybackRepeatMode.ONE) Icons.Default.RepeatOne else Icons.Default.Repeat,
                        contentDescription = "Repeat Mode",
                        tint = if (repeatMode != PlaybackRepeatMode.OFF) SportOrange else TextMuted,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Bottom Action: Lyrics Preview Trigger
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = SportCardSurface,
                border = androidx.compose.foundation.BorderStroke(1.dp, SportCardBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpenLyrics() }
                    .testTag("view_lyrics_btn")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.FormatQuote,
                            contentDescription = null,
                            tint = SportCyan,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Lirik Otomatis (LRC Synced)",
                            fontFamily = OutfitFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp,
                            color = TextWhite
                        )
                    }

                    Text(
                        text = "Buka Lirik ›",
                        fontFamily = RajdhaniFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = SportCyan
                    )
                }
            }
        }
    }
}
