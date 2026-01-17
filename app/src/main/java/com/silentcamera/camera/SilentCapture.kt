package com.silentcamera.camera

import android.content.ContentValues
import android.content.Context
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.Executor

/**
 * 무음 촬영을 담당하는 클래스
 * AudioManager를 사용하여 시스템 소리를 일시적으로 음소거합니다.
 */
class SilentCapture(private val context: Context) {

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val mainHandler = Handler(Looper.getMainLooper())

    /**
     * 무음으로 사진을 촬영합니다.
     */
    fun takeSilentPhoto(
        imageCapture: ImageCapture,
        executor: Executor,
        onImageSaved: (String) -> Unit,
        onError: (Exception) -> Unit
    ) {
        // 촬영 전 음소거
        muteSystemSound()

        // 파일명 생성
        val name = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
            .format(System.currentTimeMillis())

        // MediaStore에 저장할 ContentValues 설정
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, "IMG_$name")
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/SilentCamera")
            }
        }

        // 출력 옵션 설정
        val outputOptions = ImageCapture.OutputFileOptions.Builder(
            context.contentResolver,
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            contentValues
        ).build()

        // 사진 촬영
        imageCapture.takePicture(
            outputOptions,
            executor,
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    // 촬영 후 음소거 해제
                    unmuteSystemSound()
                    val savedUri = outputFileResults.savedUri?.toString() ?: ""
                    // 메인 스레드에서 콜백 호출
                    mainHandler.post { onImageSaved(savedUri) }
                }

                override fun onError(exception: ImageCaptureException) {
                    // 에러 발생 시에도 음소거 해제
                    unmuteSystemSound()
                    // 메인 스레드에서 콜백 호출
                    mainHandler.post { onError(exception) }
                }
            }
        )
    }

    /**
     * 시스템 소리를 음소거합니다.
     */
    @Suppress("DEPRECATION")
    private fun muteSystemSound() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                audioManager.adjustStreamVolume(
                    AudioManager.STREAM_SYSTEM,
                    AudioManager.ADJUST_MUTE,
                    0
                )
                audioManager.adjustStreamVolume(
                    AudioManager.STREAM_NOTIFICATION,
                    AudioManager.ADJUST_MUTE,
                    0
                )
            } else {
                audioManager.setStreamMute(AudioManager.STREAM_SYSTEM, true)
                audioManager.setStreamMute(AudioManager.STREAM_NOTIFICATION, true)
            }
        } catch (e: Exception) {
            // 일부 기기에서는 음소거가 제한될 수 있음
            e.printStackTrace()
        }
    }

    /**
     * 시스템 소리 음소거를 해제합니다.
     */
    @Suppress("DEPRECATION")
    private fun unmuteSystemSound() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                audioManager.adjustStreamVolume(
                    AudioManager.STREAM_SYSTEM,
                    AudioManager.ADJUST_UNMUTE,
                    0
                )
                audioManager.adjustStreamVolume(
                    AudioManager.STREAM_NOTIFICATION,
                    AudioManager.ADJUST_UNMUTE,
                    0
                )
            } else {
                audioManager.setStreamMute(AudioManager.STREAM_SYSTEM, false)
                audioManager.setStreamMute(AudioManager.STREAM_NOTIFICATION, false)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
