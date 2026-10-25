package com.opentuner.app.audio

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import kotlin.math.max

/** Streams overlapping 4096-sample mono windows (hop 2048, ~21 per second at 44.1 kHz). */
class AudioEngine(val sampleRate: Int = 44100) {
    private val window = 4096
    private val hop = 2048

    // The caller only starts listening after RECORD_AUDIO has been granted.
    @SuppressLint("MissingPermission")
    private fun open(bufBytes: Int): AudioRecord? {
        // UNPROCESSED skips AGC and noise suppression, which smear pitch; fall back to MIC where unsupported.
        for (source in intArrayOf(MediaRecorder.AudioSource.UNPROCESSED, MediaRecorder.AudioSource.MIC)) {
            val rec = AudioRecord(
                source, sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, bufBytes,
            )
            if (rec.state == AudioRecord.STATE_INITIALIZED) return rec
            rec.release()
        }
        return null
    }

    fun frames(): Flow<FloatArray> = flow {
        val minBuf = AudioRecord.getMinBufferSize(
            sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT,
        )
        if (minBuf <= 0) return@flow
        val rec = open(max(minBuf, hop * 4)) ?: return@flow

        val shorts = ShortArray(hop)
        val win = FloatArray(window)
        rec.startRecording()
        try {
            while (currentCoroutineContext().isActive) {
                var got = 0
                while (got < hop) {
                    val n = rec.read(shorts, got, hop - got)
                    if (n < 0) return@flow
                    got += n
                }
                System.arraycopy(win, hop, win, 0, window - hop)
                for (i in 0 until hop) win[window - hop + i] = shorts[i] / 32768f
                emit(win.copyOf())
            }
        } finally {
            rec.stop()
            rec.release()
        }
    }.flowOn(Dispatchers.IO)
}
