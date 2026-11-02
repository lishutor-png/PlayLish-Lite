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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextWhite
import kotlin.math.roundToInt

@Composable
fun EqualizerModal(
    currentSettings: UserSettings,
    onApplyPreset: (preset: String, bassBoost: Int) -> Unit,
    onToggleHiRes: (Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedPreset by remember { mutableStateOf(currentSettings.equalizerPreset) }
    var bassBoost by remember { mutableIntStateOf(currentSettings.bassBoostLevel) }
    var hiResEnabled by remember { mutableStateOf(currentSettings.hiResEngineEnabled) }

    // 5 graphic bands in dB (-10 to +10 dB)
    var band60Hz by remember { mutableFloatStateOf(if (selectedPreset == "Bass Boost") 6f else 2f) }
    var band230Hz by remember { mutableFloatStateOf(if (selectedPreset == "Sport Dynamic") 4f else 1f) }
    var band910Hz by remember { mutableFloatStateOf(0f) }
    var band3k6Hz by remember { mutableFloatStateOf(if (selectedPreset == "Treble") 5f else 2f) }
    var band14kHz by remember { mutableFloatStateOf(if (selectedPreset == "Hi-Res Focus") 7f else 3f) }

    val presets = listOf("Sport Dynamic", "Bass Boost", "Treble", "Vocal Focus", "Sleep Soft", "Flat")

    fun applyPresetValues(preset: String) {
        selectedPreset = preset
        when (preset) {
            "Sport Dynamic" -> {
                band60Hz = 5f; band230Hz = 3f; band910Hz = -1f; band3k6Hz = 3f; band14kHz = 5f
                bassBoost = 500
            }
            "Bass Boost" -> {
                band60Hz = 8f; band230Hz = 6f; band910Hz = 0f; band3k6Hz = -1f; band14kHz = 1f
                bassBoost = 800
            }
            "Treble" -> {
                band60Hz = -2f; band230Hz = 0f; band910Hz = 2f; band3k6Hz = 6f; band14kHz = 8f
                bassBoost = 200
            }
            "Vocal Focus" -> {
                band60Hz = -1f; band230Hz = 2f; band910Hz = 6f; band3k6Hz = 4f; band14kHz = 1f
                bassBoost = 250
            }
            "Sleep Soft" -> {
                band60Hz = 2f; band230Hz = 1f; band910Hz = -2f; band3k6Hz = -4f; band14kHz = -6f
                bassBoost = 150
            }
            "Flat" -> {
                band60Hz = 0f; band230Hz = 0f; band910Hz = 0f; band3k6Hz = 0f; band14kHz = 0f
                bassBoost = 0
            }
        }
    }

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
                // Header
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
                                .background(SportCyanDim),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Equalizer,
                                contentDescription = null,
                                tint = SportCyan,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Column {
                            Text(
                                text = "EQUALIZER & SUARA",
                                fontFamily = RajdhaniFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = TextWhite
                            )
                            Text(
                                text = "AIMP 5-Band DSP Audio Matrix",
                                fontFamily = OutfitFontFamily,
                                fontSize = 11.sp,
                                color = SportOrange
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

                Spacer(modifier = Modifier.height(16.dp))

                // Presets
                Text(
                    text = "PRESET AUDIO",
                    fontFamily = RajdhaniFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = TextMuted
                )

                Spacer(modifier = Modifier.height(8.dp))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(presets) { preset ->
                        val isSelected = selectedPreset == preset
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (isSelected) SportOrange else SportCardSurfaceVariant)
                                .border(
                                    1.dp,
                                    if (isSelected) SportOrange else SportCardBorder,
                                    RoundedCornerShape(16.dp)
                                )
                                .clickable { applyPresetValues(preset) }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                                .testTag("eq_preset_$preset")
                        ) {
                            Text(
                                text = preset,
                                fontFamily = RajdhaniFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = if (isSelected) Color.Black else TextWhite
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 5-Band Graphic EQ Visualizer & Sliders
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = SportCardSurfaceVariant),
                    border = androidx.compose.foundation.BorderStroke(1.dp, SportCardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "PITA FREKUENSI GRAFIS (dB)",
                            fontFamily = RajdhaniFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = SportCyan
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        val bands = listOf(
                            Triple("60 Hz", band60Hz) { v: Float -> band60Hz = v },
                            Triple("230 Hz", band230Hz) { v: Float -> band230Hz = v },
                            Triple("910 Hz", band910Hz) { v: Float -> band910Hz = v },
                            Triple("3.6 kHz", band3k6Hz) { v: Float -> band3k6Hz = v },
                            Triple("14 kHz", band14kHz) { v: Float -> band14kHz = v }
                        )

                        bands.forEach { (label, value, onValueChange) ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = label,
                                    fontFamily = RajdhaniFontFamily,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 12.sp,
                                    color = TextWhite,
                                    modifier = Modifier.width(60.dp)
                                )

                                Slider(
                                    value = value,
                                    onValueChange = onValueChange,
                                    valueRange = -10f..10f,
                                    colors = SliderDefaults.colors(
                                        thumbColor = SportOrange,
                                        activeTrackColor = SportOrange,
                                        inactiveTrackColor = SportCardBorder
                                    ),
                                    modifier = Modifier.weight(1f)
                                )

                                Text(
                                    text = "%+.1f dB".format(value),
                                    fontFamily = RajdhaniFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = if (value >= 0) SportOrange else SportCyan,
                                    modifier = Modifier.width(55.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Bass Boost Slider
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = SportCardSurfaceVariant),
                    border = androidx.compose.foundation.BorderStroke(1.dp, SportCardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Speed,
                                    contentDescription = null,
                                    tint = SportOrange,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "BASS BOOST (SUBWOOFER)",
                                    fontFamily = RajdhaniFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = TextWhite
                                )
                            }
                            Text(
                                text = "${bassBoost / 10}%",
                                fontFamily = RajdhaniFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = SportOrange
                            )
                        }

                        Slider(
                            value = bassBoost.toFloat(),
                            onValueChange = { bassBoost = it.roundToInt() },
                            valueRange = 0f..1000f,
                            colors = SliderDefaults.colors(
                                thumbColor = SportOrange,
                                activeTrackColor = SportOrange,
                                inactiveTrackColor = SportCardBorder
                            ),
                            modifier = Modifier.fillMaxWidth().testTag("bass_boost_slider")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Hi-Res Audio Engine Toggle
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = SportCardSurfaceVariant),
                    border = androidx.compose.foundation.BorderStroke(1.dp, SportCardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Engine Audio Hi-Res 32-Bit",
                                fontFamily = OutfitFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = TextWhite
                            )
                            Text(
                                text = "Pemrosesan internal float 32-bit untuk resolusi maksimal FLAC/WAV/ALAC.",
                                fontFamily = OutfitFontFamily,
                                fontSize = 11.sp,
                                color = TextMuted
                            )
                        }
                        Switch(
                            checked = hiResEnabled,
                            onCheckedChange = {
                                hiResEnabled = it
                                onToggleHiRes(it)
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.Black,
                                checkedTrackColor = SportCyan,
                                uncheckedThumbColor = TextMuted,
                                uncheckedTrackColor = SportCardBorder
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Save button
                Button(
                    onClick = {
                        onApplyPreset(selectedPreset, bassBoost)
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SportOrange,
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("apply_eq_btn")
                ) {
                    Text(
                        text = "Simpan Pengaturan Equalizer",
                        fontFamily = RajdhaniFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}
