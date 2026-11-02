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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Hearing
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.VolumeDown
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import com.example.ui.theme.SportCyanDim
import com.example.ui.theme.SportLime
import com.example.ui.theme.SportOrange
import com.example.ui.theme.SportOrangeDim
import com.example.ui.theme.SportRed
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextWhite
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
    var selectedMinutes by remember { mutableIntStateOf(currentSettings.sleepTimerMinutes) }
    var finishCurrentSong by remember { mutableStateOf(currentSettings.sleepModeFinishSong) }
    var volumeFade by remember { mutableStateOf(currentSettings.sleepVolumeFade) }
    var hearingProtection by remember { mutableStateOf(currentSettings.hearingProtectionEnabled) }
    var maxSafeVolume by remember { mutableIntStateOf(currentSettings.maxSafeVolumePercent) }

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
                .padding(vertical = 20.dp)
                .clip(RoundedCornerShape(24.dp))
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
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(SportOrangeDim),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Bedtime,
                                contentDescription = null,
                                tint = SportOrange,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Column {
                            Text(
                                text = "PENGATUR WAKTU TIDUR",
                                fontFamily = RajdhaniFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
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

                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = TextMuted
                        )
                    }
                }

                // Active Timer Alert if running
                if (isTimerActive) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Card(
                        colors = CardDefaults.cardColors(containerColor = SportOrangeDim),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
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
                                val mins = remainingSeconds / 60
                                val secs = remainingSeconds % 60
                                Text(
                                    text = "Sisa Waktu: %02d:%02d".format(mins, secs),
                                    fontFamily = RajdhaniFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 20.sp,
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
                                Text("Hentikan", fontFamily = RajdhaniFontFamily, fontSize = 12.sp)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 1. Duration Selection
                Text(
                    text = "DURASI TIMER: $selectedMinutes MENIT",
                    fontFamily = RajdhaniFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = SportOrange
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Quick Pills
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
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

                Spacer(modifier = Modifier.height(8.dp))

                // Slider for custom minutes (5 to 120)
                Slider(
                    value = selectedMinutes.toFloat(),
                    onValueChange = { selectedMinutes = it.roundToInt() },
                    valueRange = 5f..120f,
                    steps = 22,
                    colors = SliderDefaults.colors(
                        thumbColor = SportOrange,
                        activeTrackColor = SportOrange,
                        inactiveTrackColor = SportCardBorder
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("sleep_timer_slider")
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 2. Behavior Mode Option: Selesaikan lagu dahulu vs Langsung mati
                Text(
                    text = "MODE PENYELESAIAN WAKTU",
                    fontFamily = RajdhaniFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = TextMuted
                )

                Spacer(modifier = Modifier.height(8.dp))

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
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (finishCurrentSong) "Tunggu Lagu Selesai Dahulu" else "Langsung Berhenti Seketika",
                                    fontFamily = OutfitFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = TextWhite
                                )
                                Text(
                                    text = if (finishCurrentSong)
                                        "Ketika timer habis, lagu yang berputar akan diselesaikan hingga akhir baru pemutar mati."
                                    else
                                        "Musik langsung dihentikan detik itu juga saat waktu timer mencapai 00:00.",
                                    fontFamily = OutfitFontFamily,
                                    fontSize = 11.sp,
                                    color = TextMuted
                                )
                            }
                            Switch(
                                checked = finishCurrentSong,
                                onCheckedChange = { finishCurrentSong = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.Black,
                                    checkedTrackColor = SportOrange,
                                    uncheckedThumbColor = TextMuted,
                                    uncheckedTrackColor = SportCardBorder
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 3. Sound Dynamics: Bertahap (Fade-Out) vs Mendadak
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
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (volumeFade) "Penyesuaian Bertahap (Fade-Out)" else "Mendadak (Tetap Konstan)",
                                    fontFamily = OutfitFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = TextWhite
                                )
                                Text(
                                    text = if (volumeFade)
                                        "Volume akan melembut secara bertahap di 2 menit terakhir sehingga telinga tidak kaget."
                                    else
                                        "Volume tetap stabil dan langsung berhenti saat timer habis.",
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
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 4. Hearing Protection & Safe Decibel Limiter
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
                            text = "PENGAMANAN TELINGA & DESIBEL",
                            fontFamily = RajdhaniFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = SportCyan
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

                Spacer(modifier = Modifier.height(6.dp))

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
                            Text(
                                text = "Batas Volume Maksimal:",
                                fontFamily = OutfitFontFamily,
                                fontSize = 12.sp,
                                color = TextMuted
                            )
                            val calcDb = (20.0f * kotlin.math.log10(maxSafeVolume / 100f)).coerceIn(-30f, 0f)
                            Text(
                                text = "$maxSafeVolume% (%.1f dB)".format(calcDb),
                                fontFamily = RajdhaniFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = if (maxSafeVolume <= 75) SportLime else SportOrange
                            )
                        }

                        Slider(
                            value = maxSafeVolume.toFloat(),
                            onValueChange = {
                                maxSafeVolume = it.roundToInt()
                                if (hearingProtection) {
                                    val db = (20.0f * kotlin.math.log10(maxSafeVolume / 100f))
                                    onUpdateHearingProtection(true, maxSafeVolume, db)
                                }
                            },
                            valueRange = 30f..100f,
                            colors = SliderDefaults.colors(
                                thumbColor = SportCyan,
                                activeTrackColor = SportCyan,
                                inactiveTrackColor = SportCardBorder
                            ),
                            enabled = hearingProtection,
                            modifier = Modifier.fillMaxWidth().testTag("hearing_protection_slider")
                        )

                        Text(
                            text = "Menjaga rata-rata keluaran audio tetap seimbang dan fleksibel sehingga tetap terdengar jelas namun aman bagi telinga saat tertidur.",
                            fontFamily = OutfitFontFamily,
                            fontSize = 11.sp,
                            color = TextMuted,
                            lineHeight = 15.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Bottom CTA Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Tutup", fontFamily = RajdhaniFontFamily, fontWeight = FontWeight.Bold, color = TextWhite)
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
                        Text(
                            text = "Aktifkan Timer",
                            fontFamily = RajdhaniFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}
