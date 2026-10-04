package com.example.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Loop
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.PlaybackState
import com.example.model.ProcessingStage
import com.example.model.StemItem
import com.example.model.StemType
import com.example.ui.theme.StudioCardBg
import com.example.ui.theme.StudioCardBorder
import com.example.ui.theme.StudioCyan
import com.example.ui.theme.StudioDarkBg
import com.example.ui.theme.StudioPurple
import com.example.ui.theme.StudioSurfaceVariant
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.TextMutedDark
import com.example.ui.theme.TextPrimaryDark
import com.example.ui.theme.TextSecondaryDark

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: StemsplitterViewModel) {
    val job by viewModel.currentJob.collectAsState()
    val isProcessing by viewModel.isProcessing.collectAsState()
    val serverStatus by viewModel.serverStatus.collectAsState()
    val playbackState by viewModel.playbackState.collectAsState()
    val selectedMidiInfo by viewModel.selectedMidiInfo.collectAsState()

    var showDiagnostics by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }

    // System Audio Document Picker
    val audioPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { viewModel.onAudioSelected(it) }
    }

    val topPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val bottomPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = StudioDarkBg,
        bottomBar = {
            if (playbackState.activeStemId != null) {
                val activeStem = job?.stems?.find { it.id == playbackState.activeStemId }
                GlobalPlaybackBar(
                    stem = activeStem,
                    playbackState = playbackState,
                    onPlayToggle = {
                        activeStem?.let { viewModel.playStem(it) }
                    },
                    onSeek = { viewModel.seekPlayback(it) },
                    onToggleLoop = { viewModel.toggleLoop() },
                    onStop = { viewModel.stopPlayback() },
                    bottomPadding = bottomPadding
                )
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(top = topPadding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = if (playbackState.activeStemId != null) 90.dp else 24.dp)
        ) {
            item {
                TopHeaderBar(
                    serverConnected = serverStatus.first,
                    onOpenSettings = { showSettings = true },
                    onOpenDiagnostics = { showDiagnostics = true }
                )
            }

            item {
                AudioPickerCard(
                    metadata = job?.metadata,
                    isProcessing = isProcessing,
                    onSelectAudio = {
                        audioPickerLauncher.launch(
                            arrayOf(
                                "audio/*",
                                "audio/wav",
                                "audio/x-wav",
                                "audio/mpeg",
                                "audio/mp3",
                                "audio/flac",
                                "audio/aac",
                                "audio/mp4",
                                "audio/ogg"
                            )
                        )
                    },
                    onStartSeparation = { viewModel.startSeparation() }
                )
            }

            if (isProcessing && job != null) {
                item {
                    ProgressStatusCard(
                        stage = job!!.stage,
                        statusMessage = job!!.statusMessage,
                        progressPercent = job!!.progressPercent,
                        onCancel = { viewModel.cancelProcessing() }
                    )
                }
            }

            // Engine Checklist & Architecture Banner
            item {
                EngineSpecsBanner(
                    isSeparated = job?.stage == ProcessingStage.COMPLETED,
                    stemsCount = job?.stems?.filter { it.isDownloaded }?.size ?: 0
                )
            }

            // 6-Stem Studio Mixer & MIDI Section
            item {
                Text(
                    text = "6-STEM INSTRUMENT SUITE",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp,
                        color = TextSecondaryDark
                    ),
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                )
            }

            val stems = job?.stems
            if (stems.isNullOrEmpty()) {
                // Show placeholders for 6 stems prior to separation
                items(StemType.values().toList()) { stemType ->
                    StemPlaceholderCard(stemType = stemType)
                }
            } else {
                items(stems) { stem ->
                    StemCard(
                        stem = stem,
                        isThisStemPlaying = playbackState.activeStemId == stem.id,
                        playbackState = playbackState,
                        onPlayToggle = { viewModel.playStem(stem) },
                        onSeek = { viewModel.seekPlayback(it) },
                        onShareAudio = { viewModel.shareStemAudio(stem) },
                        onShareMidi = { viewModel.shareStemMidi(stem) },
                        onInspectMidi = { viewModel.inspectMidi(stem) }
                    )
                }
            }
        }
    }

    // MIDI Inspector Bottom Sheet
    if (selectedMidiInfo != null) {
        val (stemType, midiInfo) = selectedMidiInfo!!
        ModalBottomSheet(
            onDismissRequest = { viewModel.closeMidiInspection() },
            containerColor = StudioDarkBg,
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            MidiInspectionSheet(
                stemType = stemType,
                midiInfo = midiInfo,
                onDismiss = { viewModel.closeMidiInspection() }
            )
        }
    }

    // Diagnostics / Settings Bottom Sheet
    if (showDiagnostics || showSettings) {
        ModalBottomSheet(
            onDismissRequest = {
                showDiagnostics = false
                showSettings = false
            },
            containerColor = StudioDarkBg,
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            DiagnosticsSheet(
                logs = job?.logs ?: emptyList(),
                serverUrl = viewModel.getServerUrl(),
                onTestConnection = { viewModel.checkServer() },
                onSaveUrl = { viewModel.setCustomServerUrl(it) },
                onDismiss = {
                    showDiagnostics = false
                    showSettings = false
                }
            )
        }
    }
}

@Composable
fun EngineSpecsBanner(isSeparated: Boolean, stemsCount: Int) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        colors = CardDefaults.cardColors(containerColor = StudioCardBg),
        border = BorderStroke(1.dp, StudioCardBorder),
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isSeparated) "NEURAL EXTRACTION COMPLETE" else "PIPELINE SPECIFICATIONS",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = if (isSeparated) SuccessGreen else StudioCyan,
                        letterSpacing = 1.sp
                    )
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = if (isSeparated) "$stemsCount stems & MIDI transcriptions ready for DAW" else "HTDemucs 6-Source Model • Basic Pitch Polyphony",
                    style = MaterialTheme.typography.bodySmall.copy(color = TextSecondaryDark)
                )
            }

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = StudioSurfaceVariant,
                border = BorderStroke(1.dp, StudioCardBorder)
            ) {
                Text(
                    text = "6 STEMS",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = StudioCyan
                    ),
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
        }
    }
}

@Composable
fun StemPlaceholderCard(stemType: StemType) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 5.dp),
        colors = CardDefaults.cardColors(containerColor = StudioCardBg.copy(alpha = 0.6f)),
        border = BorderStroke(1.dp, StudioCardBorder),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(stemType.color.copy(alpha = 0.15f))
                    .border(1.dp, stemType.color.copy(alpha = 0.5f), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stemType.title.take(2).uppercase(),
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = stemType.color
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stemType.title,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextPrimaryDark
                    )
                )
                Text(
                    text = stemType.subtitle,
                    style = MaterialTheme.typography.labelSmall.copy(color = TextMutedDark)
                )
            }

            Surface(
                shape = RoundedCornerShape(6.dp),
                color = StudioSurfaceVariant
            ) {
                Text(
                    text = if (stemType == StemType.DRUMS) "GM Ch 10" else "GM Ch ${stemType.gmChannel + 1}",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = TextMutedDark,
                        fontSize = 10.sp
                    ),
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
    }
}

@Composable
fun GlobalPlaybackBar(
    stem: StemItem?,
    playbackState: PlaybackState,
    onPlayToggle: () -> Unit,
    onSeek: (Float) -> Unit,
    onToggleLoop: () -> Unit,
    onStop: () -> Unit,
    bottomPadding: androidx.compose.ui.unit.Dp
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color(0xFF0F1523),
        border = BorderStroke(1.dp, StudioCardBorder),
        shadowElevation = 8.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp)
                .padding(bottom = bottomPadding)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    if (stem != null) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(stem.stemType.color),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = stem.stemType.title.take(1),
                                color = Color.Black,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                    }

                    Column {
                        Text(
                            text = stem?.stemType?.title ?: "Stem Playback",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextPrimaryDark
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        val currentSec = playbackState.currentPositionMs / 1000
                        val totalSec = playbackState.durationMs / 1000
                        Text(
                            text = "%02d:%02d / %02d:%02d".format(
                                currentSec / 60, currentSec % 60,
                                totalSec / 60, totalSec % 60
                            ),
                            style = MaterialTheme.typography.labelSmall.copy(color = TextSecondaryDark)
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onToggleLoop) {
                        Icon(
                            imageVector = Icons.Default.Repeat,
                            contentDescription = "Loop",
                            tint = if (playbackState.isLooping) StudioCyan else TextMutedDark,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    IconButton(
                        onClick = onPlayToggle,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(StudioCyan)
                    ) {
                        Icon(
                            imageVector = if (playbackState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = "Play/Pause",
                            tint = Color.Black,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    IconButton(onClick = onStop) {
                        Icon(
                            imageVector = Icons.Default.Stop,
                            contentDescription = "Stop",
                            tint = TextSecondaryDark,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Slider(
                value = playbackState.progress,
                onValueChange = onSeek,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(20.dp),
                colors = SliderDefaults.colors(
                    thumbColor = StudioCyan,
                    activeTrackColor = StudioCyan,
                    inactiveTrackColor = StudioSurfaceVariant
                )
            )
        }
    }
}
