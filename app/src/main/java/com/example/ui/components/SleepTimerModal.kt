package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Hearing
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.UserSettings
import com.example.ui.theme.OutfitFontFamily
import com.example.ui.theme.RajdhaniFontFamily
import com.example.ui.theme.SportCardBorder
import com.example.ui.theme.SportCardSurface
import com.example.ui.theme.SportCardSurfaceVariant
import com.example.ui.theme.SportCyan
import com.example.ui.theme.SportLime
import com.example.ui.theme.SportOrange
import com.example.ui.theme.SportOrangeDim
import com.example.ui.theme.SportRed
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextWhite
import java.util.Locale
import kotlin.math.log10
import kotlin.math.roundToInt

@Composable
fun SleepTimerModal(
    currentSettings: UserSettings,
    isTimerActive: Boolean,
    remainingSeconds: Long,
    currentDecibels: Float,
    onStartTimer: (minutes: Int, finishSong: Boolean, fadeVolume: Boolean, fadeDurationMin: Int) -> Unit,
    onCancelTimer: () -> Unit,
    onUpdateHearingProtection: (enabled: Boolean, maxVolumePercent: Int, targetDb: Float) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedMinutes by remember { mutableIntStateOf(currentSettings.sleepTimerMinutes.coerceIn(5, 120)) }
    var finishCurrentSong by remember { mutableStateOf(currentSettings.sleepModeFinishSong) }
    var volumeFade by remember { mutableStateOf(currentSettings.sleepVolumeFade) }
    var hearingProtection by remember { mutableStateOf(currentSettings.hearingProtectionEnabled) }
    var maxSafeVolume by remember { mutableIntStateOf(currentSettings.maxSafeVolumePercent.coerceIn(30, 100)) }

    val quickDurations = listOf(15, 30, 45, 60, 90)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = SportCardSurface,
            border = androidx.compose.foundation.BorderStroke(1.dp, SportCardBorder),
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .padding(vertical = 16.dp)
                .clip(RoundedCornerShape(24.dp))
                .testTag("sleep_timer_modal")
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Modal Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(SportOrangeDim),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Bedtime,
                                contentDescription = null,
                                tint = SportOrange,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Column {
                            Text(
                                text = "PENGATUR WAKTU TIDUR",
                                fontFamily = RajdhaniFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp,
                                color = TextWhite
                            )
                            Text(
                                text = "Auto Sleep & Ear-Safe Limiter",
                                fontFamily = OutfitFontFamily,
                                fontSize = 11.sp,
                                color = SportCyan
                            )
                        }
                    }

                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Tutup",
                            tint = TextMuted,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // Status Bar jika timer sedang aktif berjalan
                if (isTimerActive) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Card(
                        colors = CardDefaults.cardColors(containerColor = SportOrangeDim),
                        shape = RoundedCornerShape(14.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, SportOrange),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Timer Sedang Berjalan",
                                    fontFamily = RajdhaniFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = SportOrange
                                )
                                val safeRemaining = remainingSeconds.coerceAtLeast(0L)
                                val mins = safeRemaining / 60
                                val secs = safeRemaining % 60
                                Text(
                                    text = "Sisa: %02d:%02d".format(Locale.US, mins, secs),
                                    fontFamily = RajdhaniFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 22.sp,
                                    color = TextWhite
                                )
                            }

                            Button(
                                onClick = onCancelTimer,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = SportRed,
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.testTag("cancel_sleep_timer_btn")
                            ) {
                                Text(
                                    text = "Hentikan",
                                    fontFamily = RajdhaniFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // 1. Duration Selection Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "DURASI WAKTU TIDUR",
                        fontFamily = RajdhaniFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = SportOrange
                    )
                    Text(
                        text = "$selectedMinutes Menit",
                        fontFamily = RajdhaniFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = TextWhite
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Quick Pills
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    quickDurations.forEach { mins ->
                        val isSelected = selectedMinutes == mins
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) SportOrange else SportCardSurfaceVariant)
                                .border(
                                    1.dp,
                                    if (isSelected) SportOrange else SportCardBorder,
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable { selectedMinutes = mins }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${mins}m",
                                fontFamily = RajdhaniFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = if (isSelected) Color.Black else TextWhite
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Slider for custom minutes (5 to 120)
                Slider(
                    value = selectedMinutes.toFloat(),
                    onValueChange = { selectedMinutes = it.roundToInt().coerceIn(5, 120) },
                    valueRange = 5f..120f,
                    colors = SliderDefaults.colors(
                        thumbColor = SportOrange,
                        activeTrackColor = SportOrange,
                        inactiveTrackColor = SportCardBorder
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("sleep_timer_slider")
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 2. KETIKA WAKTU HABIS (Opsi Tunggu Lagu Selesai vs Langsung Mati)
                Text(
                    text = "KETIKA WAKTU TIMER HABIS",
                    fontFamily = RajdhaniFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = SportCyan
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Opsi A: Tunggu Lagu Selesai Dahulu (Rekomendasi)
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (finishCurrentSong) SportCardSurfaceVariant else SportCardSurface
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (finishCurrentSong) SportOrange else SportCardBorder
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { finishCurrentSong = true }
                        .testTag("sleep_option_finish_song")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Tunggu Lagu Selesai Dahulu",
                                    fontFamily = OutfitFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = if (finishCurrentSong) SportOrange else TextWhite
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(SportOrange.copy(alpha = 0.2f))
                                        .padding(horizontal = 4.dp, vertical = 1.dp)
                                ) {
                                    Text(
                                        text = "Rekomendasi",
                                        fontFamily = RajdhaniFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 9.sp,
                                        color = SportOrange
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Saat timer 00:00, lagu yang sedang diputar akan diselesaikan sampai habis baru pemutar musik otomatis mati.",
                                fontFamily = OutfitFontFamily,
                                fontSize = 11.sp,
                                color = TextMuted,
                                lineHeight = 15.sp
                            )
                        }

                        RadioButton(
                            selected = finishCurrentSong,
                            onClick = { finishCurrentSong = true },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = SportOrange,
                                unselectedColor = TextMuted
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Opsi B: Langsung Berhenti Seketika
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (!finishCurrentSong) SportCardSurfaceVariant else SportCardSurface
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (!finishCurrentSong) SportCyan else SportCardBorder
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { finishCurrentSong = false }
                        .testTag("sleep_option_stop_immediately")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Langsung Berhenti Seketika",
                                fontFamily = OutfitFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = if (!finishCurrentSong) SportCyan else TextWhite
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Musik langsung berhenti seketika di detik yang sama saat waktu timer mencapai 00:00.",
                                fontFamily = OutfitFontFamily,
                                fontSize = 11.sp,
                                color = TextMuted,
                                lineHeight = 15.sp
                            )
                        }

                        RadioButton(
                            selected = !finishCurrentSong,
                            onClick = { finishCurrentSong = false },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = SportCyan,
                                unselectedColor = TextMuted
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 3. Sound Dynamics: Bertahap (Fade-Out)
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = SportCardSurfaceVariant),
                    border = androidx.compose.foundation.BorderStroke(1.dp, SportCardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (volumeFade) "Peredupan Halus (Fade-Out)" else "Tanpa Peredupan",
                                fontFamily = OutfitFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = TextWhite
                            )
                            Text(
                                text = if (volumeFade)
                                    "Volume melembut bertahap di 2 menit terakhir sehingga telinga nyaman."
                                else
                                    "Volume tetap konstan hingga akhir pemutaran.",
                                fontFamily = OutfitFontFamily,
                                fontSize = 11.sp,
                                color = TextMuted
                            )
                        }
                        Switch(
                            checked = volumeFade,
                            onCheckedChange = { volumeFade = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.Black,
                                checkedTrackColor = SportOrange,
                                uncheckedThumbColor = TextMuted,
                                uncheckedTrackColor = SportCardBorder
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 4. Hearing Protection & Safe Decibel Limiter
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = SportCardSurfaceVariant),
                    border = androidx.compose.foundation.BorderStroke(1.dp, SportCardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Hearing,
                                    contentDescription = null,
                                    tint = SportCyan,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Batas Volume Telinga Aman",
                                    fontFamily = OutfitFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = TextWhite
                                )
                            }

                            Switch(
                                checked = hearingProtection,
                                onCheckedChange = {
                                    hearingProtection = it
                                    onUpdateHearingProtection(it, maxSafeVolume, -12.0f)
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.Black,
                                    checkedTrackColor = SportCyan,
                                    uncheckedThumbColor = TextMuted,
                                    uncheckedTrackColor = SportCardBorder
                                )
                            )
                        }

                        if (hearingProtection) {
                            Spacer(modifier = Modifier.height(6.dp))
                            val ratio = (maxSafeVolume / 100f).coerceIn(0.01f, 1.0f)
                            val calcDb = (20.0f * log10(ratio)).coerceIn(-30f, 0f)
                            val dbFormatted = String.format(Locale.US, "%.1f", calcDb)

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Maksimal Output:",
                                    fontFamily = OutfitFontFamily,
                                    fontSize = 11.sp,
                                    color = TextMuted
                                )
                                Text(
                                    text = "$maxSafeVolume% ($dbFormatted dB)",
                                    fontFamily = RajdhaniFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = if (maxSafeVolume <= 75) SportLime else SportOrange
                                )
                            }

                            Slider(
                                value = maxSafeVolume.toFloat(),
                                onValueChange = {
                                    maxSafeVolume = it.roundToInt().coerceIn(30, 100)
                                    val r = (maxSafeVolume / 100f).coerceIn(0.01f, 1.0f)
                                    val db = (20.0f * log10(r))
                                    onUpdateHearingProtection(true, maxSafeVolume, db)
                                },
                                valueRange = 30f..100f,
                                colors = SliderDefaults.colors(
                                    thumbColor = SportCyan,
                                    activeTrackColor = SportCyan,
                                    inactiveTrackColor = SportCardBorder
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("hearing_protection_slider")
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Bottom Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "Tutup",
                            fontFamily = RajdhaniFontFamily,
                            fontWeight = FontWeight.Bold,
                            color = TextWhite,
                            fontSize = 13.sp
                        )
                    }

                    Button(
                        onClick = {
                            onStartTimer(
                                selectedMinutes,
                                finishCurrentSong,
                                volumeFade,
                                2 // 2 minutes fade-out
                            )
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SportOrange,
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1.5f)
                            .testTag("apply_sleep_timer_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Timer,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Aktifkan Timer",
                            fontFamily = RajdhaniFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
    }
}
