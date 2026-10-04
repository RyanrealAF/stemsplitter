package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Loop
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.MidiFileInfo
import com.example.audio.PlaybackState
import com.example.model.AudioMetadata
import com.example.model.LogEntry
import com.example.model.ProcessingStage
import com.example.model.StemItem
import com.example.model.StemType
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.StudioCardBg
import com.example.ui.theme.StudioCardBorder
import com.example.ui.theme.StudioCyan
import com.example.ui.theme.StudioDarkBg
import com.example.ui.theme.StemPianoColor
import com.example.ui.theme.StemVocalsColor
import com.example.ui.theme.StudioPurple
import com.example.ui.theme.StudioSurfaceVariant
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.TextMutedDark
import com.example.ui.theme.TextPrimaryDark
import com.example.ui.theme.TextSecondaryDark
import com.example.ui.theme.WarningAmber

@Composable
fun TopHeaderBar(
    serverConnected: Boolean?,
    onOpenSettings: () -> Unit,
    onOpenDiagnostics: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(StudioCyan, StudioPurple)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.GraphicEq,
                    contentDescription = "App Icon",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column {
                Text(
                    text = "STEMSPLITTER",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 2.sp,
                        color = TextPrimaryDark
                    )
                )
                Text(
                    text = "HTDemucs 6s • Basic Pitch MIDI",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = StudioCyan,
                        fontWeight = FontWeight.SemiBold
                    )
                )
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            // Server health pill
            val statusColor = when (serverConnected) {
                true -> SuccessGreen
                false -> ErrorRed
                null -> WarningAmber
            }
            val statusText = when (serverConnected) {
                true -> "HF Space Ready"
                false -> "Offline"
                null -> "Connecting..."
            }

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = StudioSurfaceVariant,
                border = BorderStroke(1.dp, StudioCardBorder),
                modifier = Modifier.clickable { onOpenSettings() }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(statusColor)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.labelSmall.copy(color = TextSecondaryDark)
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(
                onClick = onOpenDiagnostics,
                modifier = Modifier
                    .size(40.dp)
                    .testTag("diagnostics_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Terminal,
                    contentDescription = "Diagnostic Logs",
                    tint = TextSecondaryDark
                )
            }

            IconButton(
                onClick = onOpenSettings,
                modifier = Modifier
                    .size(40.dp)
                    .testTag("settings_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = TextSecondaryDark
                )
            }
        }
    }
}

@Composable
fun AudioPickerCard(
    metadata: AudioMetadata?,
    isProcessing: Boolean,
    onSelectAudio: () -> Unit,
    onStartSeparation: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = StudioCardBg),
        border = BorderStroke(1.dp, StudioCardBorder),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "AUDIO INPUT",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        color = StudioCyan
                    )
                )

                if (metadata != null) {
                    Text(
                        text = metadata.formattedDuration,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimaryDark
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (metadata == null) {
                // Empty state
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(StudioSurfaceVariant)
                        .clickable(enabled = !isProcessing) { onSelectAudio() }
                        .padding(vertical = 24.dp, horizontal = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Audiotrack,
                            contentDescription = "Select Audio",
                            tint = StudioCyan,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "CHOOSE AUDIO FILE",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextPrimaryDark
                            )
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "WAV, MP3, FLAC, M4A, OGG supported",
                            style = MaterialTheme.typography.bodySmall.copy(color = TextMutedDark),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                // File loaded state
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(StudioSurfaceVariant)
                        .padding(14.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.MusicNote,
                            contentDescription = null,
                            tint = StudioCyan,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = metadata.displayName,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextPrimaryDark
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = metadata.formattedSpecs,
                                style = MaterialTheme.typography.labelSmall.copy(color = TextSecondaryDark)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = onSelectAudio,
                        enabled = !isProcessing,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("change_audio_button"),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, StudioCardBorder)
                    ) {
                        Text(text = "Change File", color = TextSecondaryDark)
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Button(
                        onClick = onStartSeparation,
                        enabled = !isProcessing,
                        modifier = Modifier
                            .weight(1.5f)
                            .testTag("start_separation_button"),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = StudioCyan,
                            contentColor = Color(0xFF00363D)
                        )
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.GraphicEq,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "SEPARATE 6 STEMS",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ProgressStatusCard(
    stage: ProcessingStage,
    statusMessage: String,
    progressPercent: Float,
    onCancel: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = StudioCardBg),
        border = BorderStroke(1.dp, StudioCyan.copy(alpha = 0.4f)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = StudioCyan,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = stage.label.uppercase(),
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            color = StudioCyan
                        )
                    )
                }

                TextButton(
                    onClick = onCancel,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                ) {
                    Text(text = "Cancel", color = ErrorRed, style = MaterialTheme.typography.labelSmall)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            LinearProgressIndicator(
                progress = { progressPercent },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = StudioCyan,
                trackColor = StudioSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = statusMessage,
                style = MaterialTheme.typography.bodySmall.copy(color = TextSecondaryDark),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun StemCard(
    stem: StemItem,
    isThisStemPlaying: Boolean,
    playbackState: PlaybackState,
    onPlayToggle: () -> Unit,
    onSeek: (Float) -> Unit,
    onShareAudio: () -> Unit,
    onShareMidi: () -> Unit,
    onInspectMidi: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .testTag("stem_card_${stem.stemType.name.lowercase()}"),
        colors = CardDefaults.cardColors(containerColor = StudioCardBg),
        border = BorderStroke(
            1.dp,
            if (isThisStemPlaying) stem.stemType.color else StudioCardBorder
        ),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header Row: Color bar, Stem Title, Subtitle, Note Count Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Color accent badge
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(stem.stemType.color.copy(alpha = 0.2f))
                        .border(1.dp, stem.stemType.color, RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stem.stemType.title.take(2).uppercase(),
                        fontWeight = FontWeight.Black,
                        fontSize = 12.sp,
                        color = stem.stemType.color
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = stem.stemType.title,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextPrimaryDark
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        if (stem.fileSizeBytes > 0) {
                            Text(
                                text = stem.formattedSize,
                                style = MaterialTheme.typography.labelSmall.copy(color = TextMutedDark)
                            )
                        }
                    }

                    Text(
                        text = stem.stemType.subtitle,
                        style = MaterialTheme.typography.labelSmall.copy(color = TextSecondaryDark),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Play / Pause button
                IconButton(
                    onClick = onPlayToggle,
                    enabled = stem.isDownloaded,
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(
                            if (isThisStemPlaying) stem.stemType.color else StudioSurfaceVariant
                        )
                        .testTag("play_button_${stem.stemType.name.lowercase()}")
                ) {
                    Icon(
                        imageVector = if (isThisStemPlaying && playbackState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "Play/Pause Stem",
                        tint = if (isThisStemPlaying) Color.Black else TextPrimaryDark,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            // Audio scrubber (visible when playing)
            if (isThisStemPlaying) {
                Spacer(modifier = Modifier.height(8.dp))
                Slider(
                    value = playbackState.progress,
                    onValueChange = { onSeek(it) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(24.dp),
                    colors = SliderDefaults.colors(
                        thumbColor = stem.stemType.color,
                        activeTrackColor = stem.stemType.color,
                        inactiveTrackColor = StudioSurfaceVariant
                    )
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Footer Row: MIDI Details & Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // MIDI Info Pill
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = StudioSurfaceVariant,
                    modifier = Modifier.clickable { onInspectMidi() }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.MusicNote,
                            contentDescription = null,
                            tint = stem.stemType.color,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (stem.midiNotesCount > 0) "${stem.midiNotesCount} Notes" else "MIDI Ready",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimaryDark
                            )
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Inspect",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = StudioCyan,
                                fontSize = 10.sp
                            )
                        )
                    }
                }

                // Export buttons
                Row {
                    FilledTonalButton(
                        onClick = onShareMidi,
                        enabled = stem.localMidiPath != null,
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = StudioSurfaceVariant,
                            contentColor = TextPrimaryDark
                        ),
                        modifier = Modifier.testTag("export_midi_${stem.stemType.name.lowercase()}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = null,
                            modifier = Modifier.size(12.dp),
                            tint = StudioCyan
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "MIDI", style = MaterialTheme.typography.labelSmall)
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    FilledTonalButton(
                        onClick = onShareAudio,
                        enabled = stem.localAudioPath != null,
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = StudioSurfaceVariant,
                            contentColor = TextPrimaryDark
                        ),
                        modifier = Modifier.testTag("export_audio_${stem.stemType.name.lowercase()}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = null,
                            modifier = Modifier.size(12.dp),
                            tint = stem.stemType.color
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "Audio", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}

@Composable
fun MidiInspectionSheet(
    stemType: StemType,
    midiInfo: MidiFileInfo,
    onDismiss: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)),
        color = StudioDarkBg,
        border = BorderStroke(1.dp, StudioCardBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .clip(CircleShape)
                            .background(stemType.color)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "${stemType.title.uppercase()} MIDI INSPECTOR",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimaryDark
                        )
                    )
                }

                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = TextSecondaryDark)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Specs grid
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(StudioCardBg)
                    .padding(16.dp)
            ) {
                MidiSpecRow("General MIDI Channel", if (stemType == StemType.DRUMS) "Channel 10 (Percussion)" else "Channel ${stemType.gmChannel + 1}")
                MidiSpecRow("Detected Note Events", "${midiInfo.totalNotes} notes")
                MidiSpecRow("Time Division (PPQ)", "${midiInfo.timeDivisionPpq} ticks per quarter note")
                MidiSpecRow("Estimated Tempo", "${midiInfo.bpm} BPM")
                MidiSpecRow("SMF Format", "Format ${midiInfo.format} (${midiInfo.trackCount} track${if (midiInfo.trackCount > 1) "s" else ""})")
                MidiSpecRow("Engine", "Basic Pitch Polyphonic Neural Transcriber")
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "This standard MIDI file contains exact polyphonic note onsets, durations, and estimated velocities. Compatible with all DAWs including Ableton Live, FL Studio, Logic Pro, and Reaper.",
                style = MaterialTheme.typography.bodySmall.copy(color = TextMutedDark)
            )

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = StudioCyan, contentColor = Color.Black),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(text = "Close Inspector", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun MidiSpecRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium.copy(color = TextSecondaryDark))
        Text(text = value, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold, color = TextPrimaryDark))
    }
}

@Composable
fun DiagnosticsSheet(
    logs: List<LogEntry>,
    serverUrl: String,
    onTestConnection: () -> Unit,
    onSaveUrl: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var urlInput by remember { mutableStateOf(serverUrl) }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)),
        color = StudioDarkBg,
        border = BorderStroke(1.dp, StudioCardBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Terminal, contentDescription = null, tint = StudioCyan)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "ENGINE DIAGNOSTICS",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimaryDark
                        )
                    )
                }

                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = TextSecondaryDark)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Endpoint input
            Text(
                text = "Neural Separation Space Endpoint",
                style = MaterialTheme.typography.labelSmall.copy(color = StudioCyan, fontWeight = FontWeight.Bold)
            )
            Spacer(modifier = Modifier.height(6.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = urlInput,
                    onValueChange = { urlInput = it },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodySmall.copy(color = TextPrimaryDark, fontFamily = FontFamily.Monospace)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = {
                        onSaveUrl(urlInput)
                        onTestConnection()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StudioCyan, contentColor = Color.Black),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(text = "Connect", fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Log output list
            Text(
                text = "EXECUTION TRACE (${logs.size} events)",
                style = MaterialTheme.typography.labelSmall.copy(color = TextSecondaryDark, letterSpacing = 1.sp)
            )
            Spacer(modifier = Modifier.height(8.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF070A10))
                    .border(1.dp, StudioCardBorder, RoundedCornerShape(10.dp))
                    .padding(12.dp)
            ) {
                if (logs.isEmpty()) {
                    Text(
                        text = "No diagnostic events yet. Select an audio file to begin pipeline execution.",
                        style = MaterialTheme.typography.bodySmall.copy(color = TextMutedDark, fontFamily = FontFamily.Monospace)
                    )
                } else {
                    androidx.compose.foundation.lazy.LazyColumn {
                        items(logs.size) { index ->
                            val log = logs[index]
                            val layerColor = when (log.layer) {
                                "INPUT" -> StudioCyan
                                "NETWORK" -> StudioPurple
                                "SEPARATOR" -> StemVocalsColor
                                "TRANSCRIBER" -> StemPianoColor
                                "STORAGE" -> SuccessGreen
                                "ERROR" -> ErrorRed
                                else -> TextSecondaryDark
                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp)
                            ) {
                                Text(
                                    text = log.formattedTime,
                                    style = MaterialTheme.typography.labelSmall.copy(color = TextMutedDark, fontFamily = FontFamily.Monospace, fontSize = 10.sp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "[${log.layer}]",
                                    style = MaterialTheme.typography.labelSmall.copy(color = layerColor, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = log.message,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = if (log.isError) ErrorRed else TextPrimaryDark,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 11.sp
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
