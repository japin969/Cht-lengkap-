package com.example.util

import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import com.example.model.TrimmedAudioResult
import java.io.File
import java.io.FileOutputStream
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.Locale
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Fase 26: Native Audio Waveform Synthesizer, Analyzer, & Trimmer.
 * Supports byte-accurate 16-bit mono PCM WAV trimming as well as Android MediaExtractor/MediaMuxer
 * container trimming (.m4a/.mp4/.aac) without external third-party audio libraries.
 */
object AudioWaveTrimmer {

    const val DEFAULT_SAMPLE_RATE = 16_000
    const val DEFAULT_BAR_COUNT = 56
    private const val WAV_HEADER_BYTES = 44

    /**
     * Generates a valid RIFF/WAVE 16-bit mono PCM voice-cadence sample file on disk
     * so voice trimming & playback work immediately on both physical devices and emulators.
     */
    fun generateSampleVoiceWav(
        targetFile: File,
        durationMillis: Long = 12_000L,
        sampleRate: Int = DEFAULT_SAMPLE_RATE,
        barCount: Int = DEFAULT_BAR_COUNT
    ): List<Float> {
        val safeDurationMs = durationMillis.coerceIn(1_000L, 120_000L)
        val totalSamples = ((safeDurationMs * sampleRate) / 1000L).toInt().coerceAtLeast(sampleRate)
        val pcmBytes = ByteArray(totalSamples * 2)

        for (i in 0 until totalSamples) {
            val t = i.toDouble() / sampleRate.toDouble()
            // Natural speech-cadence envelope with syllable pauses
            val syllableEnvelope = (0.52 + 0.48 * sin(2.0 * PI * 2.4 * t)) *
                (0.65 + 0.35 * sin(2.0 * PI * 0.65 * t + 0.9))
            val pauseGate = if (sin(2.0 * PI * 0.42 * t) < -0.68) 0.08 else 1.0
            val fundamental = sin(2.0 * PI * 196.0 * t)
            val overtone1 = 0.45 * sin(2.0 * PI * 392.0 * t)
            val overtone2 = 0.22 * sin(2.0 * PI * 588.0 * t)
            val normalized = ((fundamental + overtone1 + overtone2) / 1.67) * syllableEnvelope * pauseGate
            val sampleShort = (normalized * 22_000.0).roundToInt().coerceIn(-32_767, 32_767).toShort()

            pcmBytes[i * 2] = (sampleShort.toInt() and 0xFF).toByte()
            pcmBytes[i * 2 + 1] = ((sampleShort.toInt() shr 8) and 0xFF).toByte()
        }

        targetFile.parentFile?.mkdirs()
        FileOutputStream(targetFile).use { out ->
            out.write(buildWavHeader(pcmDataLength = pcmBytes.size, sampleRate = sampleRate, channels = 1, bitsPerSample = 16))
            out.write(pcmBytes)
        }

        return extractWaveformAmplitudes(targetFile, barCount)
    }

    /**
     * Extracts normalized RMS waveform bars [0.10f .. 1.0f] from a WAV file (or deterministic
     * container size profile for compressed M4A files).
     */
    fun extractWaveformAmplitudes(file: File, barCount: Int = DEFAULT_BAR_COUNT): List<Float> {
        val count = barCount.coerceIn(8, 80)
        if (!file.exists() || file.length() <= WAV_HEADER_BYTES) {
            return List(count) { idx ->
                val v = 0.25f + 0.65f * abs(sin((idx + 1) * 0.43)).toFloat()
                v.coerceIn(0.12f, 1.0f)
            }
        }

        val isWav = try {
            RandomAccessFile(file, "r").use { raf ->
                val magic = ByteArray(4)
                raf.readFully(magic)
                String(magic, Charsets.US_ASCII) == "RIFF"
            }
        } catch (_: Exception) {
            false
        }

        if (!isWav) {
            val seed = file.length().coerceAtLeast(1L)
            return List(count) { idx ->
                val wave = abs(sin((idx + 1) * 0.51 + (seed % 17))).toFloat()
                (0.18f + 0.78f * wave).coerceIn(0.15f, 1.0f)
            }
        }

        return try {
            RandomAccessFile(file, "r").use { raf ->
                val dataBytes = (raf.length() - WAV_HEADER_BYTES).coerceAtLeast(2L)
                val totalSamples = (dataBytes / 2L).toInt().coerceAtLeast(1)
                val samplesPerBar = max(1, totalSamples / count)
                val rawRms = FloatArray(count)
                var peak = 0.01f

                for (bar in 0 until count) {
                    val startSample = bar * samplesPerBar
                    val endSample = if (bar == count - 1) totalSamples else min(totalSamples, startSample + samplesPerBar)
                    val sampleSpan = (endSample - startSample).coerceAtLeast(1)
                    val step = max(1, sampleSpan / 64)

                    var sumSq = 0.0
                    var taken = 0
                    var s = startSample
                    while (s < endSample) {
                        val bytePos = WAV_HEADER_BYTES.toLong() + (s.toLong() * 2L)
                        if (bytePos + 2L <= raf.length()) {
                            raf.seek(bytePos)
                            val lo = raf.readUnsignedByte()
                            val hi = raf.readByte().toInt()
                            val sample = (hi shl 8) or lo
                            val norm = sample.toDouble() / 32768.0
                            sumSq += norm * norm
                            taken++
                        }
                        s += step
                    }
                    val rms = if (taken > 0) sqrt(sumSq / taken).toFloat() else 0.1f
                    rawRms[bar] = rms
                    if (rms > peak) peak = rms
                }

                rawRms.map { value ->
                    (value / peak).coerceIn(0.12f, 1.0f)
                }
            }
        } catch (_: Exception) {
            List(count) { 0.35f }
        }
    }

    /**
     * Trims a local audio file (.wav or .m4a/.mp4) between [startMs] and [endMs] into [outputFile].
     */
    fun trimAudioFile(
        sourceFile: File,
        outputFile: File,
        startMs: Long,
        endMs: Long,
        barCount: Int = DEFAULT_BAR_COUNT
    ): Result<TrimmedAudioResult> {
        if (!sourceFile.exists() || sourceFile.length() == 0L) {
            return Result.failure(IllegalArgumentException("Berkas audio sumber tidak ditemukan."))
        }
        if (endMs <= startMs || (endMs - startMs) < 300L) {
            return Result.failure(IllegalArgumentException("Rentang potong minimal 0,3 detik."))
        }

        return try {
            val isWav = RandomAccessFile(sourceFile, "r").use { raf ->
                if (raf.length() < WAV_HEADER_BYTES) {
                    false
                } else {
                    val magic = ByteArray(4)
                    raf.readFully(magic)
                    String(magic, Charsets.US_ASCII) == "RIFF"
                }
            }

            val trimmedDuration = if (isWav) {
                trimPcmWavFile(sourceFile, outputFile, startMs, endMs)
            } else {
                trimCompressedMediaFile(sourceFile, outputFile, startMs, endMs)
            }

            val waveform = extractWaveformAmplitudes(outputFile, barCount = 28)
            Result.success(
                TrimmedAudioResult(
                    outputFilePath = outputFile.absolutePath,
                    durationMillis = trimmedDuration,
                    sizeBytes = outputFile.length(),
                    waveform = waveform
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun trimPcmWavFile(
        sourceFile: File,
        outputFile: File,
        startMs: Long,
        endMs: Long
    ): Long {
        RandomAccessFile(sourceFile, "r").use { raf ->
            val header = ByteArray(WAV_HEADER_BYTES)
            raf.readFully(header)
            val headerBuf = ByteBuffer.wrap(header).order(ByteOrder.LITTLE_ENDIAN)
            val channels = headerBuf.getShort(22).toInt().coerceAtLeast(1)
            val sampleRate = headerBuf.getInt(24).coerceAtLeast(8_000)
            val bitsPerSample = headerBuf.getShort(34).toInt().coerceAtLeast(16)
            val bytesPerFrame = (channels * (bitsPerSample / 8)).coerceAtLeast(2)

            val totalDataBytes = (raf.length() - WAV_HEADER_BYTES).coerceAtLeast(bytesPerFrame.toLong())
            val totalFrames = totalDataBytes / bytesPerFrame

            val startFrame = ((startMs.coerceAtLeast(0L) * sampleRate) / 1000L).coerceIn(0L, totalFrames - 1L)
            val endFrame = ((endMs * sampleRate) / 1000L).coerceIn(startFrame + 1L, totalFrames)
            val framesToCopy = (endFrame - startFrame).coerceAtLeast(1L)
            val bytesToCopy = (framesToCopy * bytesPerFrame).toInt()

            outputFile.parentFile?.mkdirs()
            FileOutputStream(outputFile).use { out ->
                out.write(
                    buildWavHeader(
                        pcmDataLength = bytesToCopy,
                        sampleRate = sampleRate,
                        channels = channels,
                        bitsPerSample = bitsPerSample
                    )
                )
                raf.seek(WAV_HEADER_BYTES.toLong() + startFrame * bytesPerFrame)
                val chunk = ByteArray(8192)
                var remaining = bytesToCopy
                while (remaining > 0) {
                    val readCount = raf.read(chunk, 0, min(chunk.size, remaining))
                    if (readCount <= 0) break
                    out.write(chunk, 0, readCount)
                    remaining -= readCount
                }
            }

            return ((framesToCopy * 1000L) / sampleRate).coerceAtLeast(300L)
        }
    }

    private fun trimCompressedMediaFile(
        sourceFile: File,
        outputFile: File,
        startMs: Long,
        endMs: Long
    ): Long {
        val extractor = MediaExtractor()
        var muxer: MediaMuxer? = null
        try {
            extractor.setDataSource(sourceFile.absolutePath)
            var audioTrackIndex = -1
            var audioFormat: MediaFormat? = null
            for (i in 0 until extractor.trackCount) {
                val format = extractor.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME).orEmpty()
                if (mime.startsWith("audio/")) {
                    audioTrackIndex = i
                    audioFormat = format
                    break
                }
            }
            if (audioTrackIndex < 0 || audioFormat == null) {
                throw IllegalStateException("Track audio tidak ditemukan pada berkas rekaman.")
            }

            extractor.selectTrack(audioTrackIndex)
            val startUs = startMs.coerceAtLeast(0L) * 1000L
            val endUs = endMs * 1000L
            extractor.seekTo(startUs, MediaExtractor.SEEK_TO_CLOSEST_SYNC)

            outputFile.parentFile?.mkdirs()
            muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            val dstTrack = muxer.addTrack(audioFormat)
            muxer.start()

            val bufferSize = if (audioFormat.containsKey(MediaFormat.KEY_MAX_INPUT_SIZE)) {
                audioFormat.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE).coerceAtLeast(65_536)
            } else {
                65_536
            }
            val byteBuffer = ByteBuffer.allocate(bufferSize)
            val bufferInfo = MediaCodec.BufferInfo()
            var firstSampleTimeUs = -1L
            var lastSampleTimeUs = startUs

            while (true) {
                bufferInfo.offset = 0
                bufferInfo.size = extractor.readSampleData(byteBuffer, 0)
                if (bufferInfo.size < 0) break

                val sampleTimeUs = extractor.sampleTime
                if (sampleTimeUs > endUs) break

                if (sampleTimeUs >= startUs) {
                    if (firstSampleTimeUs < 0L) firstSampleTimeUs = sampleTimeUs
                    bufferInfo.presentationTimeUs = (sampleTimeUs - firstSampleTimeUs).coerceAtLeast(0L)
                    bufferInfo.flags = if ((extractor.sampleFlags and MediaExtractor.SAMPLE_FLAG_SYNC) != 0) {
                        MediaCodec.BUFFER_FLAG_KEY_FRAME
                    } else {
                        0
                    }
                    muxer.writeSampleData(dstTrack, byteBuffer, bufferInfo)
                    lastSampleTimeUs = sampleTimeUs
                }
                extractor.advance()
            }

            muxer.stop()
            val actualStartUs = if (firstSampleTimeUs >= 0L) firstSampleTimeUs else startUs
            return ((lastSampleTimeUs - actualStartUs) / 1000L).coerceAtLeast(endMs - startMs)
        } finally {
            try {
                muxer?.release()
            } catch (_: Exception) {
            }
            extractor.release()
        }
    }

    fun formatDurationMillis(durationMillis: Long): String {
        val totalTenths = (durationMillis.coerceAtLeast(0L) / 100L)
        val tenths = (totalTenths % 10L).toInt()
        val totalSeconds = (totalTenths / 10L).toInt()
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return String.format(Locale.US, "%02d:%02d.%d", minutes, seconds, tenths)
    }

    fun formatCompactDurationMillis(durationMillis: Long): String {
        val totalSeconds = ((durationMillis.coerceAtLeast(0L) + 500L) / 1000L).toInt()
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return String.format(Locale.US, "%d:%02d", minutes, seconds)
    }

    private fun buildWavHeader(
        pcmDataLength: Int,
        sampleRate: Int,
        channels: Int,
        bitsPerSample: Int
    ): ByteArray {
        val byteRate = sampleRate * channels * (bitsPerSample / 8)
        val blockAlign = (channels * (bitsPerSample / 8)).toShort()
        val totalDataLen = pcmDataLength + 36

        val buffer = ByteBuffer.allocate(WAV_HEADER_BYTES).order(ByteOrder.LITTLE_ENDIAN)
        buffer.put("RIFF".toByteArray(Charsets.US_ASCII))
        buffer.putInt(totalDataLen)
        buffer.put("WAVE".toByteArray(Charsets.US_ASCII))
        buffer.put("fmt ".toByteArray(Charsets.US_ASCII))
        buffer.putInt(16) // Subchunk1Size for PCM
        buffer.putShort(1) // AudioFormat = 1 (PCM)
        buffer.putShort(channels.toShort())
        buffer.putInt(sampleRate)
        buffer.putInt(byteRate)
        buffer.putShort(blockAlign)
        buffer.putShort(bitsPerSample.toShort())
        buffer.put("data".toByteArray(Charsets.US_ASCII))
        buffer.putInt(pcmDataLength)
        return buffer.array()
    }
}
