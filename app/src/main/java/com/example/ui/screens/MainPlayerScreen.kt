package com.example.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.MusicOff
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.AddToPlaylistDialog
import com.example.ui.components.CreatePlaylistDialog
import com.example.ui.components.EqualizerModal
import com.example.ui.components.FullPlayerSheet
import com.example.ui.components.MiniPlayerBar
import com.example.ui.components.MinimalistHeader
import com.example.ui.components.PasteLyricsDialog
import com.example.ui.components.PlaylistModal
import com.example.ui.components.SettingsModal
import com.example.ui.components.SleepTimerModal
import com.example.ui.components.SortTracksDialog
import com.example.ui.components.SyncedLyricsView
import com.example.ui.components.TrackCard
import com.example.ui.theme.OutfitFontFamily
import com.example.ui.theme.RajdhaniFontFamily
import com.example.ui.theme.SportCyan
import com.example.ui.theme.SportOrange
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextWhite
import com.example.ui.viewmodel.MusicPlayerViewModel

@Composable
fun MainPlayerScreen(
    viewModel: MusicPlayerViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val userSettings by viewModel.userSettings.collectAsState()
    val filteredTracks = viewModel.getFilteredTracks()

    // File Picker for importing audio files directly
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isNotEmpty()) {
            viewModel.importAudioFiles(uris)
        }
    }

    // File Picker for importing LRC/TXT lyrics directly
    val lyricsFilePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            val targetTrack = uiState.trackToImportLyrics ?: uiState.currentTrack
            if (targetTrack != null) {
                viewModel.importLyricsFromUri(targetTrack.id, it)
            }
        }
    }

    // Permission launcher for scanning device audio
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.scanDeviceAudio()
        }
    }

    fun requestStoragePermissionAndScan() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionLauncher.launch(Manifest.permission.READ_MEDIA_AUDIO)
        } else {
            permissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }

    // Hardware Back Button Handling
    BackHandler(enabled = uiState.isFullPlayerOpen || uiState.showLyricsFullScreen) {
        if (uiState.showLyricsFullScreen) {
            viewModel.setShowLyricsFullScreen(false)
        } else if (uiState.isFullPlayerOpen) {
            viewModel.closeFullPlayer()
        }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .statusBarsPadding()
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Top Header Bar
                MinimalistHeader(
                    selectedCategory = uiState.selectedCategory,
                    onCategorySelected = { viewModel.selectCategory(it) },
                    searchQuery = uiState.searchQuery,
                    onSearchQueryChanged = { viewModel.setSearchQuery(it) },
                    isShuffleActive = uiState.isShuffleEnabled,
                    onToggleShuffle = { viewModel.toggleShuffle() },
                    onReshuffle = { viewModel.reshuffle() },
                    currentQueuePos = uiState.currentQueuePosition,
                    totalQueueCount = uiState.totalQueueCount,
                    sortByShuffle = uiState.sortQueueByShuffleOrder,
                    onToggleSortByShuffle = { viewModel.toggleSortQueueByShuffle() },
                    isSleepTimerActive = uiState.isSleepTimerActive,
                    sleepTimerSecondsRemaining = uiState.sleepTimerRemainingSeconds,
                    isWaitingForTrackEndToSleep = uiState.isWaitingForTrackEndToSleep,
                    loudnessNormalizationActive = uiState.loudnessNormalizationActive,
                    playlists = uiState.playlists,
                    selectedPlaylistId = uiState.selectedPlaylistId,
                    currentTrackSortOption = uiState.trackSortOption,
                    onOpenSortTracks = { viewModel.setShowSortDialog(true) },
                    onSelectPlaylist = { viewModel.selectPlaylist(it) },
                    onOpenPlaylistModal = { viewModel.setShowPlaylistModal(true) },
                    onCreatePlaylistClick = { viewModel.setShowCreatePlaylistDialog(true) },
                    onOpenSleepTimer = { viewModel.setShowSleepTimerDialog(true) },
                    onOpenEqualizer = { viewModel.setShowEqualizerDialog(true) },
                    onImportMusic = { filePickerLauncher.launch(arrayOf("*/*", "audio/*", "text/*", "application/octet-stream")) },
                    onOpenSettings = { viewModel.setShowSettingsDialog(true) }
                )

                // Track List
                if (filteredTracks.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(28.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(CircleShape)
                                    .background(SportCyan.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (uiState.selectedPlaylistId != null) Icons.Default.QueueMusic else Icons.Default.MusicOff,
                                    contentDescription = null,
                                    tint = if (uiState.selectedPlaylistId != null) SportCyan else SportOrange,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = if (uiState.selectedPlaylistId != null) {
                                    val plName = uiState.playlists.find { it.id == uiState.selectedPlaylistId }?.name ?: "Playlist"
                                    "Playlist \"$plName\" Masih Kosong"
                                } else if (uiState.searchQuery.isNotBlank()) {
                                    "Tidak ada lagu cocok dengan \"${uiState.searchQuery}\""
                                } else {
                                    "Belum Ada Lagu di Perpustakaan"
                                },
                                fontFamily = RajdhaniFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = TextWhite,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = if (uiState.selectedPlaylistId != null) {
                                    "Impor musik audio langsung ke playlist ini atau tambahkan dari daftar lagu Anda."
                                } else {
                                    "Semua musik bawaan telah dibersihkan. Impor file MP3/WAV/FLAC favoritmu atau pindai penyimpanan perangkat."
                                },
                                fontFamily = OutfitFontFamily,
                                fontSize = 12.sp,
                                color = TextMuted,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(20.dp))
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Button(
                                    onClick = { filePickerLauncher.launch(arrayOf("*/*", "audio/*", "text/*", "application/octet-stream")) },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = SportOrange,
                                        contentColor = Color.Black
                                    ),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.testTag("empty_state_import_btn")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Impor Lagu",
                                        fontFamily = RajdhaniFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                }

                                if (uiState.selectedPlaylistId != null) {
                                    androidx.compose.material3.OutlinedButton(
                                        onClick = { viewModel.selectPlaylist(null) },
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Text(
                                            text = "Semua Lagu",
                                            fontFamily = RajdhaniFontFamily,
                                            fontWeight = FontWeight.Bold,
                                            color = TextWhite,
                                            fontSize = 13.sp
                                        )
                                    }
                                } else {
                                    androidx.compose.material3.OutlinedButton(
                                        onClick = { requestStoragePermissionAndScan() },
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.testTag("empty_state_scan_btn")
                                    ) {
                                        Text(
                                            text = "Pindai File HP",
                                            fontFamily = RajdhaniFontFamily,
                                            fontWeight = FontWeight.Bold,
                                            color = SportCyan,
                                            fontSize = 13.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .testTag("tracks_lazy_list"),
                        contentPadding = PaddingValues(
                            start = 16.dp,
                            end = 16.dp,
                            top = 6.dp,
                            bottom = 90.dp // Leave room for floating MiniPlayerBar
                        ),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filteredTracks, key = { it.id }) { track ->
                            val orderNum = uiState.shuffleOrderMap[track.id] ?: 1
                            val isPlaying = uiState.isPlaying && uiState.currentTrack?.id == track.id

                            TrackCard(
                                track = track,
                                orderNumber = orderNum,
                                isShuffleActive = uiState.isShuffleEnabled,
                                isCurrentlyPlaying = isPlaying,
                                onTrackClick = { viewModel.playTrack(track) },
                                isFavorite = viewModel.isTrackFavorite(track.id),
                                onFavoriteToggle = { viewModel.toggleFavoriteTrack(track.id) },
                                onAddToPlaylistClick = { viewModel.promptAddToPlaylist(track) },
                                onDeleteClick = { viewModel.promptDeleteTrack(track) }
                            )
                        }
                    }
                }
            }

            // Docked MiniPlayerBar at bottom
            if (uiState.currentTrack != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    MiniPlayerBar(
                        currentTrack = uiState.currentTrack,
                        isPlaying = uiState.isPlaying,
                        currentPositionMs = uiState.currentPositionMs,
                        durationMs = uiState.durationMs,
                        orderBadge = uiState.currentQueuePosition,
                        isShuffleActive = uiState.isShuffleEnabled,
                        isSleepTimerActive = uiState.isSleepTimerActive,
                        sleepTimerSecondsRemaining = uiState.sleepTimerRemainingSeconds,
                        isWaitingForTrackEndToSleep = uiState.isWaitingForTrackEndToSleep,
                        currentDecibels = uiState.currentDecibels,
                        isFavorite = uiState.currentTrack?.let { viewModel.isTrackFavorite(it.id) } ?: false,
                        onFavoriteToggle = {
                            uiState.currentTrack?.let { viewModel.toggleFavoriteTrack(it.id) }
                        },
                        onTogglePlayPause = { viewModel.togglePlayPause() },
                        onPlayNext = { viewModel.playNext() },
                        onBarClick = { viewModel.openFullPlayer() }
                    )
                }
            }

            // Full Player Modal Screen
            AnimatedVisibility(
                visible = uiState.isFullPlayerOpen,
                enter = slideInVertically(initialOffsetY = { it }),
                exit = slideOutVertically(targetOffsetY = { it })
            ) {
                FullPlayerSheet(
                    track = uiState.currentTrack,
                    isPlaying = uiState.isPlaying,
                    currentPositionMs = uiState.currentPositionMs,
                    durationMs = uiState.durationMs,
                    orderBadge = uiState.currentQueuePosition,
                    totalQueueCount = uiState.totalQueueCount,
                    isShuffleActive = uiState.isShuffleEnabled,
                    repeatMode = uiState.repeatMode,
                    currentDecibels = uiState.currentDecibels,
                    isSleepTimerActive = uiState.isSleepTimerActive,
                    sleepTimerRemainingSeconds = uiState.sleepTimerRemainingSeconds,
                    isWaitingForTrackEndToSleep = uiState.isWaitingForTrackEndToSleep,
                    loudnessNormalizationActive = uiState.loudnessNormalizationActive,
                    appliedGainDb = uiState.appliedNormalizationGainDb,
                    currentLyrics = uiState.currentLyrics,
                    activeLyricIndex = uiState.activeLyricIndex,
                    onTogglePlayPause = { viewModel.togglePlayPause() },
                    onPlayNext = { viewModel.playNext() },
                    onPlayPrevious = { viewModel.playPrevious() },
                    onSeekTo = { viewModel.seekTo(it) },
                    onToggleShuffle = { viewModel.toggleShuffle() },
                    onReshuffle = { viewModel.reshuffle() },
                    onCycleRepeat = { viewModel.cycleRepeatMode() },
                    onOpenSleepTimer = { viewModel.setShowSleepTimerDialog(true) },
                    onOpenEqualizer = { viewModel.setShowEqualizerDialog(true) },
                    onOpenLyrics = { viewModel.setShowLyricsFullScreen(true) },
                    onImportLyrics = {
                        uiState.currentTrack?.let {
                            viewModel.promptImportLyrics(it)
                            lyricsFilePickerLauncher.launch(arrayOf("text/*", "application/octet-stream", "*/*"))
                        }
                    },
                    onPasteLyrics = {
                        uiState.currentTrack?.let {
                            viewModel.promptImportLyrics(it)
                            viewModel.setShowPasteLyricsDialog(true)
                        }
                    },
                    isFavorite = uiState.currentTrack?.let { viewModel.isTrackFavorite(it.id) } ?: false,
                    onToggleFavorite = {
                        uiState.currentTrack?.let { viewModel.toggleFavoriteTrack(it.id) }
                    },
                    onClose = { viewModel.closeFullPlayer() }
                )
            }

            // Full Lyrics View
            AnimatedVisibility(
                visible = uiState.showLyricsFullScreen && uiState.currentTrack != null,
                enter = slideInVertically(initialOffsetY = { it }),
                exit = slideOutVertically(targetOffsetY = { it })
            ) {
                uiState.currentTrack?.let { currentTrack ->
                    SyncedLyricsView(
                        track = currentTrack,
                        lyrics = uiState.currentLyrics,
                        activeLyricIndex = uiState.activeLyricIndex,
                        onSeekTo = { viewModel.seekTo(it) },
                        onClose = { viewModel.setShowLyricsFullScreen(false) },
                        onImportLyrics = {
                            viewModel.promptImportLyrics(currentTrack)
                            lyricsFilePickerLauncher.launch(arrayOf("text/*", "application/octet-stream", "*/*"))
                        },
                        onPasteLyrics = {
                            viewModel.promptImportLyrics(currentTrack)
                            viewModel.setShowPasteLyricsDialog(true)
                        }
                    )
                }
            }
        }

        // Dialogs
        if (uiState.showSleepTimerDialog) {
            SleepTimerModal(
                currentSettings = userSettings,
                isTimerActive = uiState.isSleepTimerActive,
                remainingSeconds = uiState.sleepTimerRemainingSeconds,
                currentDecibels = uiState.currentDecibels,
                onStartTimer = { mins, finishSong, fadeVolume, fadeDuration ->
                    viewModel.startSleepTimer(mins, finishSong, fadeVolume, fadeDuration)
                },
                onCancelTimer = { viewModel.cancelSleepTimer() },
                onUpdateHearingProtection = { enabled, maxVol, targetDb ->
                    viewModel.updateHearingProtection(enabled, maxVol, targetDb)
                },
                onExtendTimer = { extraMinutes ->
                    viewModel.extendSleepTimer(extraMinutes)
                },
                onDismiss = { viewModel.setShowSleepTimerDialog(false) }
            )
        }

        if (uiState.showEqualizerDialog) {
            EqualizerModal(
                currentSettings = userSettings,
                loudnessNormalizationActive = uiState.loudnessNormalizationActive,
                targetLoudnessLufs = userSettings.targetLoudnessLufs,
                appliedGainDb = uiState.appliedNormalizationGainDb,
                onApplyPreset = { preset, bassBoost ->
                    viewModel.setEqualizerPreset(preset, bassBoost)
                },
                onToggleHiRes = { viewModel.toggleHiResEngine(it) },
                onToggleLoudnessNormalization = { viewModel.toggleLoudnessNormalization(it) },
                onSetTargetLoudness = { viewModel.setTargetLoudnessLufs(it) },
                onDismiss = { viewModel.setShowEqualizerDialog(false) }
            )
        }

        if (uiState.showSettingsDialog) {
            SettingsModal(
                userSettings = userSettings,
                isScanningDevice = uiState.isScanningDevice,
                onScanDevice = { requestStoragePermissionAndScan() },
                onDismiss = { viewModel.setShowSettingsDialog(false) }
            )
        }

        // Delete Track Confirmation Dialog
        if (uiState.trackToDelete != null) {
            androidx.compose.material3.AlertDialog(
                onDismissRequest = { viewModel.cancelDeleteTrack() },
                title = {
                    Text(
                        text = "Hapus Lagu?",
                        fontFamily = RajdhaniFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = TextWhite
                    )
                },
                text = {
                    Text(
                        text = "Hapus \"${uiState.trackToDelete?.title}\" oleh ${uiState.trackToDelete?.artist} dari daftar putar?",
                        fontFamily = OutfitFontFamily,
                        fontSize = 13.sp,
                        color = TextMuted
                    )
                },
                confirmButton = {
                    Button(
                        onClick = { viewModel.confirmDeleteTrack() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = com.example.ui.theme.SportRed,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Hapus", fontFamily = RajdhaniFontFamily, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    androidx.compose.material3.OutlinedButton(
                        onClick = { viewModel.cancelDeleteTrack() },
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Batal", fontFamily = RajdhaniFontFamily, color = TextWhite)
                    }
                },
                containerColor = com.example.ui.theme.SportCardSurface,
                shape = RoundedCornerShape(16.dp)
            )
        }

        // Import Status Alert Dialog
        if (uiState.importStatusMessage != null) {
            androidx.compose.material3.AlertDialog(
                onDismissRequest = { viewModel.clearImportStatusMessage() },
                title = {
                    Text(
                        text = "Impor Lagu Berhasil",
                        fontFamily = RajdhaniFontFamily,
                        fontWeight = FontWeight.Bold,
                        color = com.example.ui.theme.SportLime
                    )
                },
                text = {
                    Text(
                        text = uiState.importStatusMessage ?: "",
                        fontFamily = OutfitFontFamily,
                        color = TextWhite
                    )
                },
                confirmButton = {
                    Button(
                        onClick = { viewModel.clearImportStatusMessage() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SportOrange,
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("OK", fontFamily = RajdhaniFontFamily, fontWeight = FontWeight.Bold)
                    }
                },
                containerColor = com.example.ui.theme.SportCardSurface,
                shape = RoundedCornerShape(16.dp)
            )
        }

        // Playlist Overview Modal
        if (uiState.showPlaylistModal) {
            val allTracksList by viewModel.allTracks.collectAsState()
            PlaylistModal(
                playlists = viewModel.getSortedPlaylists(),
                selectedPlaylistId = uiState.selectedPlaylistId,
                totalAllTracksCount = allTracksList.size,
                playlistSortOption = uiState.playlistSortOption,
                onPlaylistSortOptionChanged = { viewModel.setPlaylistSortOption(it) },
                onSelectPlaylist = { viewModel.selectPlaylist(it) },
                onCreatePlaylistClick = {
                    viewModel.setShowPlaylistModal(false)
                    viewModel.setShowCreatePlaylistDialog(true)
                },
                onPromptDeletePlaylist = { viewModel.promptDeletePlaylist(it) },
                onDismiss = { viewModel.setShowPlaylistModal(false) }
            )
        }

        // Sort Tracks Dialog
        if (uiState.showSortDialog) {
            SortTracksDialog(
                currentSortOption = uiState.trackSortOption,
                onSortOptionSelected = { viewModel.setTrackSortOption(it) },
                onDismiss = { viewModel.setShowSortDialog(false) }
            )
        }

        // Delete Playlist Confirmation Dialog
        if (uiState.playlistToDelete != null) {
            androidx.compose.material3.AlertDialog(
                onDismissRequest = { viewModel.cancelDeletePlaylist() },
                title = {
                    Text(
                        text = "Hapus Playlist?",
                        fontFamily = RajdhaniFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = TextWhite
                    )
                },
                text = {
                    Text(
                        text = "Apakah Anda yakin ingin menghapus playlist \"${uiState.playlistToDelete?.name}\"? Lagu-lagu di dalamnya tetap aman di perpustakaan musik Anda.",
                        fontFamily = OutfitFontFamily,
                        fontSize = 13.sp,
                        color = TextMuted
                    )
                },
                confirmButton = {
                    Button(
                        onClick = { viewModel.confirmDeletePlaylist() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = com.example.ui.theme.SportRed,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Hapus", fontFamily = RajdhaniFontFamily, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    androidx.compose.material3.OutlinedButton(
                        onClick = { viewModel.cancelDeletePlaylist() },
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Batal", fontFamily = RajdhaniFontFamily, color = TextWhite)
                    }
                },
                containerColor = com.example.ui.theme.SportCardSurface,
                shape = RoundedCornerShape(16.dp)
            )
        }

        // Create Playlist Dialog
        if (uiState.showCreatePlaylistDialog) {
            CreatePlaylistDialog(
                onConfirm = { name, desc ->
                    viewModel.createPlaylist(name, desc)
                },
                onDismiss = { viewModel.setShowCreatePlaylistDialog(false) }
            )
        }

        // Add Track To Playlist Dialog
        uiState.trackToAddToPlaylist?.let { track ->
            AddToPlaylistDialog(
                track = track,
                playlists = uiState.playlists,
                onAddToPlaylist = { playlistId ->
                    viewModel.addTrackToPlaylist(track.id, playlistId)
                },
                onRemoveFromPlaylist = { playlistId ->
                    viewModel.removeTrackFromPlaylist(track.id, playlistId)
                },
                onCreateNewPlaylist = {
                    viewModel.cancelAddToPlaylist()
                    viewModel.setShowCreatePlaylistDialog(true)
                },
                onDismiss = { viewModel.cancelAddToPlaylist() }
            )
        }

        // Paste / Edit Lyrics Dialog
        if (uiState.showPasteLyricsDialog) {
            val targetTrack = uiState.trackToImportLyrics ?: uiState.currentTrack
            if (targetTrack != null) {
                PasteLyricsDialog(
                    track = targetTrack,
                    onSaveLyrics = { rawText ->
                        viewModel.updateTrackLyrics(targetTrack.id, rawText)
                        viewModel.setShowPasteLyricsDialog(false)
                        viewModel.cancelImportLyrics()
                    },
                    onDismiss = {
                        viewModel.setShowPasteLyricsDialog(false)
                        viewModel.cancelImportLyrics()
                    }
                )
            }
        }
    }
}
