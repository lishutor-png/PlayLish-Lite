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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.EqualizerModal
import com.example.ui.components.FullPlayerSheet
import com.example.ui.components.MiniPlayerBar
import com.example.ui.components.MinimalistHeader
import com.example.ui.components.SettingsModal
import com.example.ui.components.SleepTimerModal
import com.example.ui.components.SyncedLyricsView
import com.example.ui.components.TrackCard
import com.example.ui.theme.OutfitFontFamily
import com.example.ui.theme.RajdhaniFontFamily
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
                    loudnessNormalizationActive = uiState.loudnessNormalizationActive,
                    onOpenSleepTimer = { viewModel.setShowSleepTimerDialog(true) },
                    onOpenEqualizer = { viewModel.setShowEqualizerDialog(true) },
                    onImportMusic = { filePickerLauncher.launch(arrayOf("audio/*")) },
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
                            modifier = Modifier.padding(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.MusicOff,
                                contentDescription = null,
                                tint = TextMuted,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Tidak ada lagu yang cocok",
                                fontFamily = OutfitFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = TextMuted
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Pindai musik dari penyimpanan perangkat atau pilih kategori lain.",
                                fontFamily = OutfitFontFamily,
                                fontSize = 12.sp,
                                color = TextMuted,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = { requestStoragePermissionAndScan() },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = SportOrange,
                                    contentColor = Color.Black
                                ),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "Pindai File Perangkat",
                                    fontFamily = RajdhaniFontFamily,
                                    fontWeight = FontWeight.Bold
                                )
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
                        currentDecibels = uiState.currentDecibels,
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
                    loudnessNormalizationActive = uiState.loudnessNormalizationActive,
                    appliedGainDb = uiState.appliedNormalizationGainDb,
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
                        onClose = { viewModel.setShowLyricsFullScreen(false) }
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
    }
}
