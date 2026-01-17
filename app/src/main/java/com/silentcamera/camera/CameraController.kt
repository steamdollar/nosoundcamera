package com.silentcamera.camera

import android.content.Context
import android.util.Log
import android.view.MotionEvent
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageCapture
import androidx.camera.core.MeteringPoint
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * CameraX를 사용한 카메라 제어 클래스
 */
class CameraController(private val context: Context) {

    private var cameraProvider: ProcessCameraProvider? = null
    private var imageCapture: ImageCapture? = null
    private var preview: Preview? = null
    private var camera: androidx.camera.core.Camera? = null
    private var cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
    private var isUsingBackCamera = true
    private var previewView: PreviewView? = null


    private val cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    private val silentCapture = SilentCapture(context)

    companion object {
        private const val TAG = "CameraController"
    }

    /**
     * 카메라를 시작합니다.
     */
    fun startCamera(
        lifecycleOwner: LifecycleOwner,
        previewView: PreviewView,
        onReady: () -> Unit = {},
        onError: (Exception) -> Unit = {}
    ) {
        this.previewView = previewView
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

        cameraProviderFuture.addListener({
            try {
                cameraProvider = cameraProviderFuture.get()

                // Preview 설정
                preview = Preview.Builder()
                    .build()
                    .also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }

                // ImageCapture 설정
                imageCapture = ImageCapture.Builder()
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                    .build()

                // 카메라 바인딩
                bindCamera(lifecycleOwner)
                onReady()

            } catch (e: Exception) {
                Log.e(TAG, "Camera initialization failed", e)
                onError(e)
            }
        }, ContextCompat.getMainExecutor(context))
    }

    /**
     * 카메라를 lifecycle에 바인딩합니다.
     */
    private fun bindCamera(lifecycleOwner: LifecycleOwner) {
        try {
            cameraProvider?.unbindAll()
            camera = cameraProvider?.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                preview,
                imageCapture
            )
        } catch (e: Exception) {
            Log.e(TAG, "Camera binding failed", e)
        }
    }

    /**
     * 특정 지점에 초점을 맞춥니다.
     */
    fun setFocus(x: Float, y: Float) {
        val currentPreviewView = previewView ?: return

        val factory = currentPreviewView.meteringPointFactory
        val point: MeteringPoint = factory.createPoint(x, y)
        val action: FocusMeteringAction = FocusMeteringAction.Builder(point,
            FocusMeteringAction.FLAG_AF or
            FocusMeteringAction.FLAG_AE or
            FocusMeteringAction.FLAG_AWB
        )
            .setAutoCancelDuration(5, TimeUnit.SECONDS)
            .build()
        camera?.cameraControl?.startFocusAndMetering(action)
    }


    /**
     * 현재 줌 비율을 반환합니다. (0.0 ~ 1.0)
     */
    fun getZoomRatio(): Float {
        return camera?.cameraInfo?.zoomState?.value?.linearZoom ?: 0f
    }

    /**
     * 줌을 설정합니다. (0.0 ~ 1.0)
     */
    fun setZoom(linearZoom: Float) {
        camera?.cameraControl?.setLinearZoom(linearZoom.coerceIn(0f, 1f))
    }

    /**
     * 전면/후면 카메라를 전환합니다.
     */
    fun switchCamera(lifecycleOwner: LifecycleOwner) {
        isUsingBackCamera = !isUsingBackCamera
        cameraSelector = if (isUsingBackCamera) {
            CameraSelector.DEFAULT_BACK_CAMERA
        } else {
            CameraSelector.DEFAULT_FRONT_CAMERA
        }
        bindCamera(lifecycleOwner)
    }

    /**
     * 현재 후면 카메라를 사용 중인지 확인합니다.
     */
    fun isUsingBackCamera(): Boolean = isUsingBackCamera

    /**
     * 무음으로 사진을 촬영합니다.
     */
    fun takePicture(
        onImageSaved: (String) -> Unit,
        onError: (Exception) -> Unit
    ) {
        val capture = imageCapture ?: run {
            onError(IllegalStateException("ImageCapture is not initialized"))
            return
        }

        silentCapture.takeSilentPhoto(
            imageCapture = capture,
            executor = cameraExecutor,
            onImageSaved = onImageSaved,
            onError = onError
        )
    }

    /**
     * 리소스를 해제합니다.
     */
    fun shutdown() {
        cameraExecutor.shutdown()
    }
}
