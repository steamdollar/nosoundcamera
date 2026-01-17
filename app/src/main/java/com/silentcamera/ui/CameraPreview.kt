package com.silentcamera.ui

import android.annotation.SuppressLint
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import androidx.camera.view.PreviewView
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

/**
 * CameraX PreviewView를 Compose에서 사용하기 위한 래퍼 컴포넌트
 * 핀치 줌 제스처를 지원합니다.
 */
@SuppressLint("ClickableViewAccessibility")
@Composable
fun CameraPreview(
    modifier: Modifier = Modifier,
    onPreviewViewCreated: (PreviewView) -> Unit,
    onPinchZoom: ((Float) -> Unit)? = null
) {
    AndroidView(
        modifier = modifier,
        factory = { context ->
            PreviewView(context).apply {
                implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                scaleType = PreviewView.ScaleType.FILL_CENTER

                // 핀치 줌 제스처 감지
                if (onPinchZoom != null) {
                    val scaleGestureDetector = ScaleGestureDetector(
                        context,
                        object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
                            override fun onScale(detector: ScaleGestureDetector): Boolean {
                                onPinchZoom(detector.scaleFactor)
                                return true
                            }
                        }
                    )

                    setOnTouchListener { _, event ->
                        scaleGestureDetector.onTouchEvent(event)
                        true
                    }
                }

                onPreviewViewCreated(this)
            }
        }
    )
}
