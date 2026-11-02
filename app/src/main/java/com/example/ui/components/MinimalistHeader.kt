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
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shuffle
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
    onOpenSleepTimer: () -> Unit,
    onOpenEqualizer: () -> Unit,
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
                // Sleep Timer Button with Live Badge
                IconButton(
                    onClick = onOpenSleepTimer,
                    modifier = Modifier.testTag("sleep_timer_top_btn")
                ) {
                    if (isSleepTimerActive) {
                        BadgedBox(
                            badge = {
                                val mins = sleepTimerSecondsRemaining / 60
                                val secs = sleepTimerSecondsRemaining % 60
                                Badge(
                                    containerColor = SportOrange,
                                    contentColor = Color.Black
                                ) {
                                    Text(
                                        text = "%d:%02d".format(mins, secs),
                                        fontFamily = RajdhaniFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp
                                    )
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Bedtime,
                                contentDescription = "Sleep Timer Active",
                                tint = SportOrange
                            )
                        }
                    } else {
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

        // Categories chip row
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(categories) { category ->
                val isSelected = category == selectedCategory
                FilterChip(
                    selected = isSelected,
                    onClick = { onCategorySelected(category) },
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
                // Shuffle Status Indicator
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
                        Text(
                            text = if (isShuffleActive) "MODE ACAK BERURUTAN" else "URUTAN NORMAL",
                            fontFamily = RajdhaniFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = if (isShuffleActive) SportOrange else TextMuted
                        )
                        if (isShuffleActive) {
                            Text(
                                text = "Lagu #$currentQueuePos dari $totalQueueCount",
                                fontFamily = RajdhaniFontFamily,
                                fontSize = 11.sp,
                                color = SportCyan
                            )
                        }
                    }
                }

                // Actions: Reshuffle (Acak Ulang) & Sort Toggle
                Row(verticalAlignment = Alignment.CenterVertically) {
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

                        // Toggle sort by shuffle order
                        IconButton(
                            onClick = onToggleSortByShuffle,
                            modifier = Modifier.size(28.dp).testTag("sort_queue_btn")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Sort,
                                contentDescription = "Urutkan Antrean",
                                tint = if (sortByShuffle) SportCyan else TextMuted,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
