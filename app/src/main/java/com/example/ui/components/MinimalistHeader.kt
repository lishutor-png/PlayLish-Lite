package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shuffle
import com.example.data.model.Playlist
import com.example.data.model.TrackSortOption
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

@Composable
fun MinimalistHeader(
    selectedCategory: String,
    onCategorySelected: (String) -> Unit,
    searchQuery: String,
    onSearchQueryChanged: (String) -> Unit,
    isShuffleActive: Boolean,
    onToggleShuffle: () -> Unit,
    onReshuffle: () -> Unit,
    currentQueuePos: Int,
    totalQueueCount: Int,
    sortByShuffle: Boolean,
    onToggleSortByShuffle: () -> Unit,
    isSleepTimerActive: Boolean,
    sleepTimerSecondsRemaining: Long,
    isWaitingForTrackEndToSleep: Boolean = false,
    loudnessNormalizationActive: Boolean,
    playlists: List<Playlist> = emptyList(),
    selectedPlaylistId: String? = null,
    currentTrackSortOption: TrackSortOption = TrackSortOption.DEFAULT,
    onOpenSortTracks: () -> Unit = {},
    onSelectPlaylist: (String?) -> Unit,
    onOpenPlaylistModal: () -> Unit,
    onCreatePlaylistClick: () -> Unit,
    onOpenSleepTimer: () -> Unit,
    onOpenEqualizer: () -> Unit,
    onImportMusic: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isSearchExpanded by remember { mutableStateOf(false) }

    val categories = listOf("Semua", "Hi-Res", "Enerjik", "Pengantar Tidur", "Perangkat")

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // Top row: App branding + Quick Action Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Sporty Emblem
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(SportOrange),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.GraphicEq,
                        contentDescription = "PlayLish Lite Logo",
                        tint = Color.Black,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "PlayLish",
                            fontFamily = RajdhaniFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = TextWhite
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "LITE",
                            fontFamily = RajdhaniFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = SportOrange,
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(SportOrangeDim)
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                    Text(
                        text = "AIMP SYSTEM • HI-RES ENGINE",
                        fontFamily = RajdhaniFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 10.sp,
                        color = SportCyan,
                        letterSpacing = 0.5.sp
                    )
                }
            }

            // Quick action icons (Sleep Timer with ticker badge, Equalizer, Search, Settings)
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Sleep Timer Button with Live High-Contrast Capsule Badge
                if (isSleepTimerActive || isWaitingForTrackEndToSleep) {
                    val badgeText = if (isWaitingForTrackEndToSleep) {
                        "Lagu Selesai"
                    } else {
                        val hours = sleepTimerSecondsRemaining / 3600
                        val mins = (sleepTimerSecondsRemaining % 3600) / 60
                        val secs = sleepTimerSecondsRemaining % 60
                        if (hours > 0) "%d:%02d:%02d".format(hours, mins, secs)
                        else "%02d:%02d".format(mins, secs)
                    }
                    Surface(
                        onClick = onOpenSleepTimer,
                        shape = RoundedCornerShape(20.dp),
                        color = SportOrange,
                        contentColor = Color.Black,
                        shadowElevation = 3.dp,
                        modifier = Modifier
                            .padding(end = 6.dp)
                            .testTag("sleep_timer_top_btn")
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
                        modifier = Modifier.testTag("sleep_timer_top_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bedtime,
                            contentDescription = "Sleep Timer",
                            tint = TextMuted
                        )
                    }
                }

                // Equalizer button
                IconButton(
                    onClick = onOpenEqualizer,
                    modifier = Modifier.testTag("equalizer_top_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Equalizer,
                        contentDescription = "Equalizer",
                        tint = TextWhite
                    )
                }

                // Playlist button
                IconButton(
                    onClick = onOpenPlaylistModal,
                    modifier = Modifier.testTag("playlist_top_btn")
                ) {
                    if (selectedPlaylistId != null) {
                        BadgedBox(
                            badge = {
                                Badge(
                                    containerColor = SportOrange,
                                    contentColor = Color.Black
                                )
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.QueueMusic,
                                contentDescription = "Daftar Playlist",
                                tint = SportOrange
                            )
                        }
                    } else {
                        Icon(
                            imageVector = Icons.Default.QueueMusic,
                            contentDescription = "Daftar Playlist",
                            tint = TextWhite
                        )
                    }
                }

                // Import Music button
                IconButton(
                    onClick = onImportMusic,
                    modifier = Modifier.testTag("import_music_top_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Impor Lagu",
                        tint = SportCyan
                    )
                }

                // Search toggle
                IconButton(
                    onClick = { isSearchExpanded = !isSearchExpanded },
                    modifier = Modifier.testTag("search_toggle_btn")
                ) {
                    Icon(
                        imageVector = if (isSearchExpanded) Icons.Default.Close else Icons.Default.Search,
                        contentDescription = "Search",
                        tint = if (isSearchExpanded) SportCyan else TextWhite
                    )
                }

                // Settings button
                IconButton(
                    onClick = onOpenSettings,
                    modifier = Modifier.testTag("settings_top_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = TextWhite
                    )
                }
            }
        }

        // Expandable search bar
        AnimatedVisibility(visible = isSearchExpanded) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChanged,
                placeholder = { Text("Cari lagu, artis, atau album...", fontSize = 13.sp) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
                    .testTag("search_input_field"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = SportOrange,
                    unfocusedBorderColor = SportCardBorder,
                    focusedTextColor = TextWhite,
                    unfocusedTextColor = TextWhite,
                    focusedContainerColor = SportCardSurface,
                    unfocusedContainerColor = SportCardSurface
                ),
                shape = RoundedCornerShape(12.dp)
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        val activePlaylist = playlists.find { it.id == selectedPlaylistId }

        // Playlist and Categories chip row
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            // "Semua Lagu" Chip
            val isAllSelected = selectedPlaylistId == null && selectedCategory == "Semua"
            item {
                FilterChip(
                    selected = isAllSelected,
                    onClick = {
                        onSelectPlaylist(null)
                        onCategorySelected("Semua")
                    },
                    label = {
                        Text(
                            text = "Semua Lagu",
                            fontSize = 12.sp,
                            fontWeight = if (isAllSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = SportOrange,
                        selectedLabelColor = Color.Black,
                        containerColor = SportCardSurface,
                        labelColor = TextMuted
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        borderColor = if (isAllSelected) SportOrange else SportCardBorder,
                        selectedBorderColor = SportOrange,
                        enabled = true,
                        selected = isAllSelected
                    ),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.testTag("chip_all_tracks")
                )
            }

            // Playlist Chips
            items(playlists, key = { it.id }) { playlist ->
                val isSelected = selectedPlaylistId == playlist.id
                FilterChip(
                    selected = isSelected,
                    onClick = { onSelectPlaylist(playlist.id) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.QueueMusic,
                            contentDescription = null,
                            tint = if (isSelected) Color.Black else SportCyan,
                            modifier = Modifier.size(14.dp)
                        )
                    },
                    label = {
                        Text(
                            text = "${playlist.name} (${playlist.trackIds.size})",
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = SportCyan,
                        selectedLabelColor = Color.Black,
                        containerColor = SportCardSurface,
                        labelColor = TextWhite
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        borderColor = if (isSelected) SportCyan else SportCardBorder,
                        selectedBorderColor = SportCyan,
                        enabled = true,
                        selected = isSelected
                    ),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.testTag("chip_playlist_${playlist.id}")
                )
            }

            // "+ Buat Playlist" Chip
            item {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color.Transparent,
                    border = androidx.compose.foundation.BorderStroke(1.dp, SportOrange.copy(alpha = 0.5f)),
                    modifier = Modifier
                        .clickable { onCreatePlaylistClick() }
                        .testTag("chip_create_playlist")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            tint = SportOrange,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Playlist",
                            fontSize = 12.sp,
                            fontFamily = RajdhaniFontFamily,
                            fontWeight = FontWeight.Bold,
                            color = SportOrange
                        )
                    }
                }
            }

            // Category Chips (Hi-Res, Perangkat)
            listOf("Hi-Res", "Perangkat").forEach { category ->
                val isSelected = selectedPlaylistId == null && selectedCategory == category
                item {
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            onSelectPlaylist(null)
                            onCategorySelected(category)
                        },
                        label = {
                            Text(
                                text = category,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = SportOrange,
                            selectedLabelColor = Color.Black,
                            containerColor = SportCardSurface,
                            labelColor = TextMuted
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            borderColor = if (isSelected) SportOrange else SportCardBorder,
                            selectedBorderColor = SportOrange,
                            enabled = true,
                            selected = isSelected
                        ),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.testTag("category_chip_$category")
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Ordered Shuffle HUD Bar
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = SportCardSurface,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Shuffle Status Indicator & Active Playlist
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { onToggleShuffle() }
                ) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(if (isShuffleActive) SportOrange else SportCardBorder),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shuffle,
                            contentDescription = "Shuffle Mode",
                            tint = if (isShuffleActive) Color.Black else TextMuted,
                            modifier = Modifier.size(14.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Column {
                        val headerLabel = if (activePlaylist != null) {
                            "PLAYLIST: ${activePlaylist.name.uppercase()}"
                        } else if (isShuffleActive) {
                            "MODE ACAK BERURUTAN"
                        } else {
                            "URUTAN NORMAL"
                        }
                        Text(
                            text = headerLabel,
                            fontFamily = RajdhaniFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = if (activePlaylist != null) SportCyan else if (isShuffleActive) SportOrange else TextMuted
                        )
                        Text(
                            text = if (totalQueueCount > 0) "Lagu #$currentQueuePos dari $totalQueueCount" else "0 lagu dalam antrean",
                            fontFamily = RajdhaniFontFamily,
                            fontSize = 11.sp,
                            color = if (isShuffleActive) SportCyan else TextMuted
                        )
                    }
                }

                // Actions: AVL badge, Reshuffle (Acak Ulang) & Sort Toggle
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (loudnessNormalizationActive) {
                        Text(
                            text = "AVL ON",
                            fontFamily = RajdhaniFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            color = SportLime,
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(SportLime.copy(alpha = 0.15f))
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                    }

                    if (isShuffleActive) {
                        // Reshuffle button with animated new order numbers
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(SportOrangeDim)
                                .clickable { onReshuffle() }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                .testTag("reshuffle_button")
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

                        Spacer(modifier = Modifier.width(6.dp))
                    }

                    // Sort Button (Open SortTracksDialog)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (currentTrackSortOption != TrackSortOption.DEFAULT) SportCyan.copy(alpha = 0.2f) else SportCardSurfaceVariant)
                            .clickable { onOpenSortTracks() }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                            .testTag("sort_tracks_top_btn")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Sort,
                            contentDescription = "Urutkan Lagu",
                            tint = if (currentTrackSortOption != TrackSortOption.DEFAULT) SportCyan else TextMuted,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (currentTrackSortOption != TrackSortOption.DEFAULT) currentTrackSortOption.displayName else "Sortir",
                            fontFamily = RajdhaniFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = if (currentTrackSortOption != TrackSortOption.DEFAULT) SportCyan else TextWhite
                        )
                    }
                }
            }
        }
    }
}
