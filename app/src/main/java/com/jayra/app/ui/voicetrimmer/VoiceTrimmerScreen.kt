package com.jayra.app.ui.voicetrimmer

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ContentCut
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jayra.app.data.ChatRepository
import com.jayra.app.di.AppModule
import com.jayra.app.model.VoiceTrimmerStrings
import com.jayra.app.model.VoiceTrimmerUiState
import com.jayra.app.ui.theme.JayraBlue
import com.jayra.app.ui.theme.JayraCardDark
import com.jayra.app.ui.theme.JayraCardLight
import com.jayra.app.ui.theme.JayraDangerRed
import com.jayra.app.ui.theme.JayraDarkChatBg
import com.jayra.app.ui.theme.JayraDarkTopBar
import com.jayra.app.ui.theme.JayraLightChatBg
import com.jayra.app.ui.theme.JayraLightTopBar
import com.jayra.app.ui.theme.JayraOrange
import com.jayra.app.ui.theme.JayraSubtext
import com.jayra.app.ui.theme.JayraSuccessGreen
import com.jayra.app.ui.theme.JayraTheme
import com.jayra.app.util.AudioWaveTrimmer
import java.io.File
import java.util.UUID
import kotlin.math.abs
import kotlin.math.sin
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

class VoiceTrimmerViewModel @JvmOverloads constructor(
    application: Application,
    private val chatRepository: ChatRepository = AppModule.chatRepository
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow<VoiceTrimmerUiState>(VoiceTrimmerUiState.Loading)
    val uiState: StateFlow<VoiceTrimmerUiState> = _uiState.asStateFlow()

    private val _strings = MutableStateFlow(VoiceTrimmerStrings())
    val strings: StateFlow<VoiceTrimmerStrings> = _strings.asStateFlow()

    private var previewPlayer: MediaPlayer? = null
    private var previewProgressJob: Job? = null
    private var mediaRecorder: MediaRecorder? = null
    private var recordingTimerJob: Job? = null
    private var activeRecordingFile: File? = null

    init {
        prepareDefaultSampleVoice()
    }

    fun prepareDefaultSampleVoice() {
        stopPreviewPlaybackInternal()
        stopRecorderQuietly()
        _uiState.value = VoiceTrimmerUiState.Loading

        viewModelScope.launch {
            try {
                val app = getApplication<Application>()
                val sampleFile = File(app.cacheDir, "voice_trimmer_sample.wav")
                val defaultDurationMs = 12_000L
                val bars = withContext(Dispatchers.IO) {
                    AudioWaveTrimmer.generateSampleVoiceWav(
                        targetFile = sampleFile,
                        durationMillis = defaultDurationMs,
                        barCount = AudioWaveTrimmer.DEFAULT_BAR_COUNT
                    )
                }
                _uiState.value = VoiceTrimmerUiState.Ready(
                    sourceFilePath = sampleFile.absolutePath,
                    totalDurationMillis = defaultDurationMs,
                    startFraction = 0.15f,
                    endFraction = 0.78f,
                    waveform = bars,
                    isPlayingPreview = false,
                    playheadFraction = 0.15f
                )
            } catch (e: Exception) {
                Timber.e(e, "Gagal menyiapkan berkas awal VoiceTrimmer")
                _uiState.value = VoiceTrimmerUiState.Error(
                    e.localizedMessage ?: "Gagal memuat gelombang suara."
                )
            }
        }
    }

    fun onRangeChange(newStart: Float, newEnd: Float) {
        val current = _uiState.value as? VoiceTrimmerUiState.Ready ?: return
        if (current.isRecordingMic) return

        val minSpan = minFractionGap(current.totalDurationMillis)
        val clampedStart = newStart.coerceIn(0f, (1f - minSpan).coerceAtLeast(0f))
        val clampedEnd = newEnd.coerceIn((clampedStart + minSpan).coerceAtMost(1f), 1f)

        stopPreviewPlaybackInternal()
        _uiState.value = current.copy(
            startFraction = clampedStart,
            endFraction = clampedEnd,
            isPlayingPreview = false,
            playheadFraction = clampedStart
        )
    }

    fun onStartFractionChange(newStart: Float) {
        val current = _uiState.value as? VoiceTrimmerUiState.Ready ?: return
        if (current.isRecordingMic) return

        val minSpan = minFractionGap(current.totalDurationMillis)
        val maxAllowedStart = (current.endFraction - minSpan).coerceAtLeast(0f)
        val clampedStart = newStart.coerceIn(0f, maxAllowedStart)

        stopPreviewPlaybackInternal()
        _uiState.value = current.copy(
            startFraction = clampedStart,
            isPlayingPreview = false,
            playheadFraction = clampedStart
        )
    }

    fun onEndFractionChange(newEnd: Float) {
        val current = _uiState.value as? VoiceTrimmerUiState.Ready ?: return
        if (current.isRecordingMic) return

        val minSpan = minFractionGap(current.totalDurationMillis)
        val minAllowedEnd = (current.startFraction + minSpan).coerceAtMost(1f)
        val clampedEnd = newEnd.coerceIn(minAllowedEnd, 1f)

        stopPreviewPlaybackInternal()
        _uiState.value = current.copy(
            endFraction = clampedEnd,
            isPlayingPreview = false,
            playheadFraction = current.startFraction
        )
    }

    fun togglePreviewPlayback() {
        val current = _uiState.value as? VoiceTrimmerUiState.Ready ?: return
        if (current.isRecordingMic) return

        if (current.isPlayingPreview) {
            stopPreviewPlaybackInternal()
            _uiState.value = current.copy(
                isPlayingPreview = false,
                playheadFraction = current.startFraction
            )
            return
        }

        val sourceFile = File(current.sourceFilePath)
        val startMs = current.startMillis.toInt()
        val endMs = current.endMillis.toInt()

        try {
            stopPreviewPlaybackInternal()
            if (sourceFile.exists() && sourceFile.length() > 44L) {
                val mp = MediaPlayer().apply {
                    setDataSource(sourceFile.absolutePath)
                    prepare()
                    seekTo(startMs)
                    start()
                }
                previewPlayer = mp
            }

            _uiState.value = current.copy(
                isPlayingPreview = true,
                playheadFraction = current.startFraction
            )

            previewProgressJob = viewModelScope.launch {
                val segmentDurationMs = (endMs - startMs).coerceAtLeast(300)
                var simulatedElapsed = 0L
                while (true) {
                    val stateNow = _uiState.value as? VoiceTrimmerUiState.Ready ?: break
                    if (!stateNow.isPlayingPreview) break

                    val player = previewPlayer
                    val posMs = if (player != null) {
                        try {
                            player.currentPosition.toLong()
                        } catch (_: Exception) {
                            startMs + simulatedElapsed
                        }
                    } else {
                        startMs + simulatedElapsed
                    }

                    if (posMs >= endMs) {
                        stopPreviewPlaybackInternal()
                        _uiState.value = stateNow.copy(
                            isPlayingPreview = false,
                            playheadFraction = stateNow.startFraction
                        )
                        break
                    }

                    val fraction = (posMs.toFloat() / stateNow.totalDurationMillis.coerceAtLeast(1L).toFloat())
                        .coerceIn(stateNow.startFraction, stateNow.endFraction)
                    _uiState.value = stateNow.copy(playheadFraction = fraction)

                    delay(40L)
                    simulatedElapsed = (simulatedElapsed + 40L).coerceAtMost(segmentDurationMs.toLong())
                }
            }
        } catch (e: Exception) {
            Timber.w(e, "Pratinjau MediaPlayer gagal, menggunakan simulasi playhead")
            stopPreviewPlaybackInternal()
        }
    }

    fun toggleMicRecording() {
        val current = _uiState.value as? VoiceTrimmerUiState.Ready ?: return
        if (current.isRecordingMic) {
            finishMicRecording()
        } else {
            startMicRecording(current)
        }
    }

    private fun startMicRecording(current: VoiceTrimmerUiState.Ready) {
        stopPreviewPlaybackInternal()
        stopRecorderQuietly()

        val app = getApplication<Application>()
        val recFile = File(app.cacheDir, "voice_recorded_${System.currentTimeMillis()}.m4a")
        activeRecordingFile = recFile

        try {
            val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(app)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }
            recorder.setAudioSource(MediaRecorder.AudioSource.MIC)
            recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            recorder.setAudioSamplingRate(16_000)
            recorder.setAudioEncodingBitRate(64_000)
            recorder.setOutputFile(recFile.absolutePath)
            recorder.prepare()
            recorder.start()
            mediaRecorder = recorder

            _uiState.value = current.copy(
                isPlayingPreview = false,
                isRecordingMic = true,
                recordingElapsedMillis = 0L
            )

            recordingTimerJob = viewModelScope.launch {
                val startedAt = System.currentTimeMillis()
                while (true) {
                    delay(100L)
                    val ready = _uiState.value as? VoiceTrimmerUiState.Ready ?: break
                    if (!ready.isRecordingMic) break
                    val elapsed = System.currentTimeMillis() - startedAt
                    _uiState.value = ready.copy(recordingElapsedMillis = elapsed)
                }
            }
        } catch (e: Exception) {
            Timber.w(e, "MediaRecorder tidak didukung di lingkungan saat ini; tetap gunakan contoh WAV")
            stopRecorderQuietly()
            _uiState.value = current.copy(isRecordingMic = false)
        }
    }

    private fun finishMicRecording() {
        val current = _uiState.value as? VoiceTrimmerUiState.Ready ?: return
        val elapsedMs = current.recordingElapsedMillis.coerceAtLeast(1_500L)
        recordingTimerJob?.cancel()
        recordingTimerJob = null

        var recordedSuccessfully = false
        try {
            mediaRecorder?.stop()
            recordedSuccessfully = true
        } catch (e: Exception) {
            Timber.w(e, "Gagal menghentikan MediaRecorder")
        } finally {
            try {
                mediaRecorder?.release()
            } catch (_: Exception) {
            }
            mediaRecorder = null
        }

        val recFile = activeRecordingFile
        if (recordedSuccessfully && recFile != null && recFile.exists() && recFile.length() > 128L) {
            viewModelScope.launch {
                val bars = withContext(Dispatchers.IO) {
                    AudioWaveTrimmer.extractWaveformAmplitudes(recFile, AudioWaveTrimmer.DEFAULT_BAR_COUNT)
                }
                _uiState.value = VoiceTrimmerUiState.Ready(
                    sourceFilePath = recFile.absolutePath,
                    totalDurationMillis = elapsedMs,
                    startFraction = 0.0f,
                    endFraction = 1.0f,
                    waveform = bars,
                    isPlayingPreview = false,
                    playheadFraction = 0.0f,
                    isRecordingMic = false,
                    recordingElapsedMillis = 0L
                )
            }
        } else {
            _uiState.value = current.copy(isRecordingMic = false, recordingElapsedMillis = 0L)
        }
    }

    fun saveAndSendVoiceNote(chatId: String, onSuccess: () -> Unit) {
        val current = _uiState.value as? VoiceTrimmerUiState.Ready ?: return
        if (current.isRecordingMic) return

        stopPreviewPlaybackInternal()
        _uiState.value = VoiceTrimmerUiState.Saving

        viewModelScope.launch {
            try {
                val app = getApplication<Application>()
                val sourceFile = File(current.sourceFilePath)
                val extension = if (sourceFile.extension.equals("m4a", ignoreCase = true)) "m4a" else "wav"
                val outDir = File(app.filesDir, "trimmed_voices").apply { mkdirs() }
                val outFile = File(outDir, "voice_${UUID.randomUUID()}.$extension")

                val trimResult = withContext(Dispatchers.IO) {
                    if (!sourceFile.exists()) {
                        AudioWaveTrimmer.generateSampleVoiceWav(sourceFile, current.totalDurationMillis)
                    }
                    AudioWaveTrimmer.trimAudioFile(
                        sourceFile = sourceFile,
                        outputFile = outFile,
                        startMs = current.startMillis,
                        endMs = current.endMillis
                    ).getOrThrow()
                }

                chatRepository.sendVoiceNoteMessage(
                    chatId = chatId,
                    filePath = trimResult.outputFilePath,
                    durationMillis = trimResult.durationMillis,
                    waveform = trimResult.waveform
                )

                onSuccess()
            } catch (e: Exception) {
                Timber.e(e, "Gagal memotong dan mengirim Voice Note")
                _uiState.value = VoiceTrimmerUiState.Error(
                    e.localizedMessage ?: "Gagal memotong berkas suara."
                )
            }
        }
    }

    private fun minFractionGap(totalDurationMillis: Long): Float {
        if (totalDurationMillis <= 0L) return 0.1f
        return (MIN_TRIM_DURATION_MS.toFloat() / totalDurationMillis.toFloat()).coerceIn(0.05f, 0.5f)
    }

    private fun stopPreviewPlaybackInternal() {
        previewProgressJob?.cancel()
        previewProgressJob = null
        try {
            previewPlayer?.stop()
        } catch (_: Exception) {
        }
        try {
            previewPlayer?.release()
        } catch (_: Exception) {
        }
        previewPlayer = null
    }

    private fun stopRecorderQuietly() {
        recordingTimerJob?.cancel()
        recordingTimerJob = null
        try {
            mediaRecorder?.stop()
        } catch (_: Exception) {
        }
        try {
            mediaRecorder?.release()
        } catch (_: Exception) {
        }
        mediaRecorder = null
    }

    override fun onCleared() {
        stopPreviewPlaybackInternal()
        stopRecorderQuietly()
        super.onCleared()
    }

    companion object {
        const val MIN_TRIM_DURATION_MS = 800L
    }
}

@Composable
fun VoiceTrimmerScreen(
    chatId: String,
    onBack: () -> Unit,
    viewModel: VoiceTrimmerViewModel = viewModel()
) {
    BackHandler { onBack() }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val strings by viewModel.strings.collectAsStateWithLifecycle()

    val recordPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            viewModel.toggleMicRecording()
        } else {
            scope.launch {
                snackbarHostState.showSnackbar(strings.permissionDeniedMessage)
            }
        }
    }

    VoiceTrimmerContent(
        uiState = uiState,
        strings = strings,
        snackbarHostState = snackbarHostState,
        onBack = onBack,
        onRangeChange = viewModel::onRangeChange,
        onStartFractionChange = viewModel::onStartFractionChange,
        onEndFractionChange = viewModel::onEndFractionChange,
        onTogglePreview = viewModel::togglePreviewPlayback,
        onToggleRecordMic = {
            val ready = uiState as? VoiceTrimmerUiState.Ready
            if (ready?.isRecordingMic == true) {
                viewModel.toggleMicRecording()
            } else {
                val hasPerm = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.RECORD_AUDIO
                ) == PackageManager.PERMISSION_GRANTED
                if (hasPerm) {
                    viewModel.toggleMicRecording()
                } else {
                    recordPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                }
            }
        },
        onResetSample = viewModel::prepareDefaultSampleVoice,
        onSaveClick = {
            viewModel.saveAndSendVoiceNote(chatId = chatId, onSuccess = onBack)
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceTrimmerContent(
    uiState: VoiceTrimmerUiState,
    strings: VoiceTrimmerStrings,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    onBack: () -> Unit,
    onRangeChange: (Float, Float) -> Unit,
    onStartFractionChange: (Float) -> Unit,
    onEndFractionChange: (Float) -> Unit,
    onTogglePreview: () -> Unit,
    onToggleRecordMic: () -> Unit,
    onResetSample: () -> Unit,
    onSaveClick: () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val screenBg = if (isDark) JayraDarkChatBg else JayraLightChatBg
    val topBarBg = if (isDark) JayraDarkTopBar else JayraLightTopBar
    val cardBg = if (isDark) JayraCardDark else JayraCardLight
    val primaryTextColor = if (isDark) Color(0xFFFFFFFF) else Color(0xFF1C1C1E)

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        containerColor = screenBg,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(topBarBg)
                    .statusBarsPadding()
                    .height(56.dp)
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("voice_trimmer_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = strings.backContentDescription,
                        tint = Color(0xFFFFFFFF),
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = strings.screenTitle,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFFFFFFFF),
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = Icons.Rounded.ContentCut,
                    contentDescription = null,
                    tint = Color(0xFFFFFFFF).copy(alpha = 0.85f),
                    modifier = Modifier
                        .padding(end = 12.dp)
                        .size(22.dp)
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(screenBg)
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            when (uiState) {
                is VoiceTrimmerUiState.Loading,
                is VoiceTrimmerUiState.Saving -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(
                            color = JayraBlue,
                            strokeWidth = 3.dp,
                            modifier = Modifier.size(40.dp)
                        )
                        if (uiState is VoiceTrimmerUiState.Saving) {
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = strings.savingLabel,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium,
                                color = JayraSubtext
                            )
                        }
                    }
                }

                is VoiceTrimmerUiState.Error -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = uiState.message,
                            fontSize = 15.sp,
                            color = JayraDangerRed,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Box(
                            modifier = Modifier
                                .defaultMinSize(minWidth = 120.dp, minHeight = 48.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(JayraBlue)
                                .clickable(onClick = onResetSample)
                                .padding(horizontal = 20.dp, vertical = 12.dp)
                                .testTag("voice_trimmer_retry_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = strings.retryLabel,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFFFFFFFF)
                            )
                        }
                    }
                }

                is VoiceTrimmerUiState.Ready -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 600.dp)
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp, vertical = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // 1. Waveform Visualizer Card
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(cardBg)
                                .padding(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(JayraBlue.copy(alpha = 0.14f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.GraphicEq,
                                        contentDescription = null,
                                        tint = JayraBlue,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = strings.waveformHeaderTitle,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = primaryTextColor
                                    )
                                    Text(
                                        text = strings.waveformSubtitle,
                                        fontSize = 13.sp,
                                        color = JayraSubtext
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            WaveformTrimmerCanvas(
                                waveform = uiState.waveform,
                                startFraction = uiState.startFraction,
                                endFraction = uiState.endFraction,
                                playheadFraction = uiState.playheadFraction,
                                isPlaying = uiState.isPlayingPreview,
                                isDark = isDark
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            // Duration Chips Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                DurationStatPill(
                                    label = strings.startSliderLabel,
                                    value = AudioWaveTrimmer.formatDurationMillis(uiState.startMillis),
                                    accentColor = JayraOrange
                                )
                                DurationStatPill(
                                    label = strings.trimmedDurationLabel,
                                    value = AudioWaveTrimmer.formatDurationMillis(uiState.trimmedDurationMillis),
                                    accentColor = JayraSuccessGreen
                                )
                                DurationStatPill(
                                    label = strings.endSliderLabel,
                                    value = AudioWaveTrimmer.formatDurationMillis(uiState.endMillis),
                                    accentColor = JayraOrange
                                )
                            }
                        }

                        // 2. Dual Sliders Card (RangeSlider + Start & End Precision Sliders)
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(cardBg)
                                .padding(horizontal = 16.dp, vertical = 14.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = strings.rangeSliderLabel,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = primaryTextColor
                                )
                                Text(
                                    text = "${strings.totalDurationLabel}: ${AudioWaveTrimmer.formatDurationMillis(uiState.totalDurationMillis)}",
                                    fontSize = 12.sp,
                                    color = JayraSubtext
                                )
                            }

                            RangeSlider(
                                value = uiState.startFraction..uiState.endFraction,
                                onValueChange = { range ->
                                    onRangeChange(range.start, range.endInclusive)
                                },
                                valueRange = 0f..1f,
                                colors = SliderDefaults.colors(
                                    thumbColor = JayraBlue,
                                    activeTrackColor = JayraBlue,
                                    inactiveTrackColor = JayraSubtext.copy(alpha = 0.25f)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("voice_trimmer_range_slider")
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            // Slider 1: Start Handle
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = strings.startSliderLabel,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = primaryTextColor
                                )
                                Text(
                                    text = AudioWaveTrimmer.formatDurationMillis(uiState.startMillis),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = JayraBlue
                                )
                            }
                            Slider(
                                value = uiState.startFraction,
                                onValueChange = onStartFractionChange,
                                valueRange = 0f..1f,
                                colors = SliderDefaults.colors(
                                    thumbColor = JayraOrange,
                                    activeTrackColor = JayraOrange,
                                    inactiveTrackColor = JayraSubtext.copy(alpha = 0.22f)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("voice_trimmer_start_slider")
                            )

                            // Slider 2: End Handle
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = strings.endSliderLabel,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = primaryTextColor
                                )
                                Text(
                                    text = AudioWaveTrimmer.formatDurationMillis(uiState.endMillis),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = JayraBlue
                                )
                            }
                            Slider(
                                value = uiState.endFraction,
                                onValueChange = onEndFractionChange,
                                valueRange = 0f..1f,
                                colors = SliderDefaults.colors(
                                    thumbColor = JayraOrange,
                                    activeTrackColor = JayraBlue,
                                    inactiveTrackColor = JayraSubtext.copy(alpha = 0.22f)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("voice_trimmer_end_slider")
                            )
                        }

                        // 3. Playback & Microphone Action Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            val playBtnBg by animateColorAsState(
                                targetValue = if (uiState.isPlayingPreview) {
                                    JayraSuccessGreen
                                } else {
                                    JayraBlue.copy(alpha = 0.14f)
                                },
                                animationSpec = tween(durationMillis = 200),
                                label = "preview_play_bg"
                            )
                            val playBtnTextColor = if (uiState.isPlayingPreview) {
                                Color(0xFFFFFFFF)
                            } else {
                                JayraBlue
                            }

                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .defaultMinSize(minHeight = 48.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(playBtnBg)
                                    .clickable(onClick = onTogglePreview)
                                    .padding(horizontal = 12.dp, vertical = 12.dp)
                                    .testTag("voice_trimmer_preview_button"),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (uiState.isPlayingPreview) {
                                        Icons.Rounded.Pause
                                    } else {
                                        Icons.Rounded.PlayArrow
                                    },
                                    contentDescription = null,
                                    tint = playBtnTextColor,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (uiState.isPlayingPreview) {
                                        strings.previewPauseLabel
                                    } else {
                                        strings.previewPlayLabel
                                    },
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = playBtnTextColor
                                )
                            }

                            val micBtnBg by animateColorAsState(
                                targetValue = if (uiState.isRecordingMic) {
                                    JayraDangerRed
                                } else {
                                    cardBg
                                },
                                animationSpec = tween(durationMillis = 200),
                                label = "mic_btn_bg"
                            )
                            val micTextColor = if (uiState.isRecordingMic) {
                                Color(0xFFFFFFFF)
                            } else {
                                primaryTextColor
                            }

                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .defaultMinSize(minHeight = 48.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(micBtnBg)
                                    .clickable(onClick = onToggleRecordMic)
                                    .padding(horizontal = 12.dp, vertical = 12.dp)
                                    .testTag("voice_trimmer_record_button"),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (uiState.isRecordingMic) {
                                        Icons.Rounded.Stop
                                    } else {
                                        Icons.Rounded.Mic
                                    },
                                    contentDescription = null,
                                    tint = if (uiState.isRecordingMic) Color(0xFFFFFFFF) else JayraDangerRed,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (uiState.isRecordingMic) {
                                        "${strings.stopRecordLabel} (${AudioWaveTrimmer.formatCompactDurationMillis(uiState.recordingElapsedMillis)})"
                                    } else {
                                        strings.recordMicLabel
                                    },
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = micTextColor
                                )
                            }

                            IconButton(
                                onClick = onResetSample,
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(cardBg)
                                    .testTag("voice_trimmer_reset_sample_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Refresh,
                                    contentDescription = strings.resetSampleLabel,
                                    tint = JayraSubtext,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }

                        // 4. Primary "Simpan" Button (Cuts & Sends Trimmed Voice Note)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(JayraBlue)
                                .clickable(
                                    enabled = !uiState.isRecordingMic,
                                    onClick = onSaveClick
                                )
                                .testTag("save_trimmed_voice_button"),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Check,
                                contentDescription = null,
                                tint = Color(0xFFFFFFFF),
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "${strings.saveButtonLabel} (${AudioWaveTrimmer.formatCompactDurationMillis(uiState.trimmedDurationMillis)})",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFFFFFFFF)
                            )
                        }
                    }
                }
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose { }
    }
}

@Composable
private fun WaveformTrimmerCanvas(
    waveform: List<Float>,
    startFraction: Float,
    endFraction: Float,
    playheadFraction: Float,
    isPlaying: Boolean,
    isDark: Boolean
) {
    val bars = remember(waveform) {
        if (waveform.isNotEmpty()) {
            waveform
        } else {
            List(AudioWaveTrimmer.DEFAULT_BAR_COUNT) { idx ->
                (0.20f + 0.75f * abs(sin((idx + 1) * 0.44))).toFloat().coerceIn(0.16f, 1.0f)
            }
        }
    }

    val containerBg = if (isDark) Color(0xFF151E27) else Color(0xFFF2F6FA)
    val inactiveBarColor = if (isDark) Color(0xFFFFFFFF).copy(alpha = 0.20f) else JayraSubtext.copy(alpha = 0.30f)
    val selectionTint = JayraBlue.copy(alpha = if (isDark) 0.16f else 0.10f)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(132.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(containerBg)
            .padding(horizontal = 10.dp, vertical = 8.dp)
            .testTag("voice_trimmer_waveform_canvas")
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val clampedStart = startFraction.coerceIn(0f, 1f)
            val clampedEnd = endFraction.coerceIn(clampedStart, 1f)
            val startX = size.width * clampedStart
            val endX = size.width * clampedEnd

            // Highlighted trim window background
            drawRoundRect(
                color = selectionTint,
                topLeft = Offset(startX, 0f),
                size = Size((endX - startX).coerceAtLeast(2.dp.toPx()), size.height),
                cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx())
            )

            // Waveform bars using DrawScope size.width & size.height
            val count = bars.size.coerceAtLeast(1)
            val gapPx = 3.dp.toPx()
            val totalGaps = gapPx * (count - 1)
            val barWidth = ((size.width - totalGaps) / count).coerceAtLeast(2.dp.toPx())
            val barCorner = CornerRadius(barWidth / 2f, barWidth / 2f)
            val maxBarHeight = size.height * 0.82f

            bars.forEachIndexed { index, amp ->
                val centerFraction = (index + 0.5f) / count.toFloat()
                val inTrimWindow = centerFraction in clampedStart..clampedEnd
                val barHeight = (maxBarHeight * amp.coerceIn(0.14f, 1.0f)).coerceAtLeast(5.dp.toPx())
                val topY = (size.height - barHeight) / 2f
                val leftX = index * (barWidth + gapPx)

                drawRoundRect(
                    color = if (inTrimWindow) JayraBlue else inactiveBarColor,
                    topLeft = Offset(leftX, topY),
                    size = Size(barWidth, barHeight),
                    cornerRadius = barCorner
                )
            }

            // Left & right trim boundary handles
            val handleWidth = 3.dp.toPx()
            val knobWidth = 10.dp.toPx()
            val knobHeight = 28.dp.toPx()

            drawRect(
                color = JayraOrange,
                topLeft = Offset((startX - handleWidth / 2f).coerceAtLeast(0f), 0f),
                size = Size(handleWidth, size.height)
            )
            drawRoundRect(
                color = JayraOrange,
                topLeft = Offset(
                    (startX - knobWidth / 2f).coerceIn(0f, (size.width - knobWidth).coerceAtLeast(0f)),
                    (size.height - knobHeight) / 2f
                ),
                size = Size(knobWidth, knobHeight),
                cornerRadius = CornerRadius(5.dp.toPx(), 5.dp.toPx())
            )

            drawRect(
                color = JayraOrange,
                topLeft = Offset((endX - handleWidth / 2f).coerceIn(0f, (size.width - handleWidth).coerceAtLeast(0f)), 0f),
                size = Size(handleWidth, size.height)
            )
            drawRoundRect(
                color = JayraOrange,
                topLeft = Offset(
                    (endX - knobWidth / 2f).coerceIn(0f, (size.width - knobWidth).coerceAtLeast(0f)),
                    (size.height - knobHeight) / 2f
                ),
                size = Size(knobWidth, knobHeight),
                cornerRadius = CornerRadius(5.dp.toPx(), 5.dp.toPx())
            )

            // Active Playhead Indicator
            if (isPlaying || playheadFraction > clampedStart + 0.005f) {
                val playheadX = (size.width * playheadFraction.coerceIn(clampedStart, clampedEnd))
                drawLine(
                    color = JayraSuccessGreen,
                    start = Offset(playheadX, 4.dp.toPx()),
                    end = Offset(playheadX, size.height - 4.dp.toPx()),
                    strokeWidth = 2.5.dp.toPx()
                )
            }
        }
    }
}

@Composable
private fun DurationStatPill(
    label: String,
    value: String,
    accentColor: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            fontSize = 11.sp,
            color = JayraSubtext
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = accentColor
        )
    }
}

@Preview(name = "Voice Trimmer Screen Light", showBackground = true)
@Composable
private fun VoiceTrimmerScreenLightPreview() {
    JayraTheme(darkTheme = false) {
        Surface {
            VoiceTrimmerContent(
                uiState = VoiceTrimmerUiState.Ready(
                    sourceFilePath = "/cache/sample_voice.wav",
                    totalDurationMillis = 12_000L,
                    startFraction = 0.18f,
                    endFraction = 0.76f,
                    waveform = List(56) { idx ->
                        (0.20f + 0.76f * abs(sin((idx + 1) * 0.42))).toFloat()
                    },
                    isPlayingPreview = true,
                    playheadFraction = 0.44f
                ),
                strings = VoiceTrimmerStrings(),
                onBack = {},
                onRangeChange = { _, _ -> },
                onStartFractionChange = {},
                onEndFractionChange = {},
                onTogglePreview = {},
                onToggleRecordMic = {},
                onResetSample = {},
                onSaveClick = {}
            )
        }
    }
}

@Preview(name = "Voice Trimmer Screen Dark", showBackground = true)
@Composable
private fun VoiceTrimmerScreenDarkPreview() {
    JayraTheme(darkTheme = true) {
        Surface {
            VoiceTrimmerContent(
                uiState = VoiceTrimmerUiState.Ready(
                    sourceFilePath = "/cache/sample_voice.wav",
                    totalDurationMillis = 15_400L,
                    startFraction = 0.12f,
                    endFraction = 0.68f,
                    waveform = List(56) { idx ->
                        (0.22f + 0.74f * abs(sin((idx + 2) * 0.39))).toFloat()
                    },
                    isPlayingPreview = false,
                    playheadFraction = 0.12f
                ),
                strings = VoiceTrimmerStrings(),
                onBack = {},
                onRangeChange = { _, _ -> },
                onStartFractionChange = {},
                onEndFractionChange = {},
                onTogglePreview = {},
                onToggleRecordMic = {},
                onResetSample = {},
                onSaveClick = {}
            )
        }
    }
}
