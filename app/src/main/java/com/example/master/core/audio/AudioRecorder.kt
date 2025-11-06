package com.example.master.core.audio

import android.content.Context
import android.media.MediaRecorder
import java.io.File

/**
 * Basic audio recorder for capturing user pronunciation exercises.
 */
class AudioRecorder(@Suppress("UNUSED_PARAMETER") context: Context) {
    
    private var recorder: MediaRecorder? = null
    private var outputFile: File? = null

    fun startRecording(file: File) {
        stopRecording()
        outputFile = file
        recorder = MediaRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setAudioSamplingRate(44100)
            setAudioEncodingBitRate(96000)
            setOutputFile(file.absolutePath)
            prepare()
            start()
        }
    }

    fun stopRecording(): File? {
        recorder?.apply {
            try {
                stop()
            } catch (_: RuntimeException) {
                // Ignore stop failures when the recorder wasn't started
            }
            reset()
            release()
        }
        recorder = null
        return outputFile
    }
}
