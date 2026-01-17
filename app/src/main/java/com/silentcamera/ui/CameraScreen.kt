package com.silentcamera.ui

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.TimerOff
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.silentcamera.camera.CameraController
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * 메인 카메라 화면 Composable
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CameraScreen(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var previewView by remember { mutableStateOf<PreviewView?>(null) }
    val cameraController = remember { CameraController(context) }
    var zoomLevel by remember { mutableFloatStateOf(0f) }
    var lastCapturedUri by remember { mutableStateOf<Uri?>(null) }
    var tappedOffset by remember { mutableStateOf<Offset?>(null) }
    val coroutineScope = rememberCoroutineScope()


    // 타이머 관련 상태
    var timerSeconds by remember { mutableIntStateOf(0) }
    var countdownSeconds by remember { mutableIntStateOf(0) }
    var isCountingDown by remember { mutableStateOf(false) }
    var showTimerDropdown by remember { mutableStateOf(false) }

    val timerOptions = listOf(0, 2, 3, 5, 10, 15, 30)

    val onImageSaved: (String) -> Unit = { uriString ->
        lastCapturedUri = Uri.parse(uriString)
        Toast.makeText(context, "사진이 갤러리에 저장되었습니다.", Toast.LENGTH_SHORT).show()
    }

    val onError: (Exception) -> Unit = { e ->
        Toast.makeText(context, "촬영 실패: ${e.message}", Toast.LENGTH_SHORT).show()
    }

    // 카운트다운 로직
    LaunchedEffect(isCountingDown, countdownSeconds) {
        if (isCountingDown && countdownSeconds > 0) {
            delay(1000L)
            countdownSeconds--
        } else if (isCountingDown && countdownSeconds == 0) {
            isCountingDown = false
            cameraController.takePicture(onImageSaved, onError)
        }
    }

    // previewView가 설정되면 카메라 시작
    DisposableEffect(Unit) {
        onDispose {
            cameraController.shutdown()
        }
    }

    // previewView가 변경될 때 카메라 시작
    DisposableEffect(previewView) {
        previewView?.let { preview ->
            cameraController.startCamera(
                lifecycleOwner = lifecycleOwner,
                previewView = preview,
                onError = { e ->
                    Toast.makeText(context, "카메라 오류: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            )
        }
        onDispose { }
    }

    Box(modifier = modifier.fillMaxSize()) {
        // 카메라 프리뷰
        CameraPreview(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = { offset ->
                            cameraController.setFocus(offset.x, offset.y)
                            coroutineScope.launch {
                                tappedOffset = offset
                                delay(1000)
                                tappedOffset = null
                            }
                        }
                    )
                }
                .pointerInput(Unit) {
                    detectTransformGestures { _, _, zoom, _ ->
                        val newZoomLevel = zoomLevel + (zoom - 1f) * 0.5f
                        zoomLevel = newZoomLevel.coerceIn(0f, 1f)
                        cameraController.setZoom(zoomLevel)
                    }
                },
            onPreviewViewCreated = { previewView = it }
        )

        // 터치 포커스 피드백 UI
        AnimatedVisibility(
            visible = tappedOffset != null,
            enter = fadeIn(animationSpec = androidx.compose.animation.core.tween(durationMillis = 100)) + scaleIn(
                initialScale = 1.3f,
                animationSpec = androidx.compose.animation.core.tween(durationMillis = 150)
            ),
            exit = fadeOut(animationSpec = androidx.compose.animation.core.tween(delayMillis = 200, durationMillis = 200))
        ) {
            tappedOffset?.let {
                Box(
                    modifier = Modifier
                        .offset(
                            x = (it.x / LocalContext.current.resources.displayMetrics.density) .dp - 32.dp,
                            y = (it.y / LocalContext.current.resources.displayMetrics.density).dp - 32.dp
                        )
                        .size(64.dp)
                        .border(1.dp, Color.White, CircleShape)
                )
            }
        }


        // 카운트다운 표시 (개선된 UI)
        AnimatedVisibility(
            visible = isCountingDown && countdownSeconds > 0,
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut()
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                // 배경 원
                Box(
                    modifier = Modifier
                        .size(140.dp)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.6f),
                                    Color.Black.copy(alpha = 0.3f),
                                    Color.Transparent
                                )
                            ),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = countdownSeconds.toString(),
                        color = Color.White,
                        fontSize = 72.sp,
                        fontWeight = FontWeight.Light
                    )
                }

                // 취소 버튼
                IconButton(
                    onClick = {
                        isCountingDown = false
                        countdownSeconds = 0
                    },
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 100.dp)
                        .size(48.dp)
                        .background(Color.Red.copy(alpha = 0.8f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "촬영 취소",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }


        // 하단 컨트롤 영역
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .background(Color.Black.copy(alpha = 0.3f))
                .padding(top = 20.dp, bottom = 80.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 36.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 타이머 버튼 (드롭다운 메뉴)
                Box {
                    IconButton(
                        onClick = { showTimerDropdown = true },
                        modifier = Modifier
                            .size(48.dp)
                            .background(
                                if (timerSeconds > 0) Color.Yellow.copy(alpha = 0.3f)
                                else Color.White.copy(alpha = 0.2f),
                                CircleShape
                            )
                    ) {
                        if (timerSeconds == 0) {
                            Icon(
                                imageVector = Icons.Default.TimerOff,
                                contentDescription = "타이머 끔",
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                        } else {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Timer,
                                    contentDescription = "타이머 ${timerSeconds}초",
                                    tint = Color.Yellow,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "${timerSeconds}s",
                                    color = Color.Yellow,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // 드롭다운 메뉴
                    DropdownMenu(
                        expanded = showTimerDropdown,
                        onDismissRequest = { showTimerDropdown = false },
                        modifier = Modifier.background(Color.DarkGray)
                    ) {
                        timerOptions.forEach { seconds ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = if (seconds == 0) "타이머 끔" else "${seconds}초",
                                        color = if (seconds == timerSeconds) Color.Yellow else Color.White
                                    )
                                },
                                onClick = {
                                    timerSeconds = seconds
                                    showTimerDropdown = false
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = if (seconds == 0) Icons.Default.TimerOff else Icons.Default.Timer,
                                        contentDescription = null,
                                        tint = if (seconds == timerSeconds) Color.Yellow else Color.White
                                    )
                                }
                            )
                        }
                    }
                }

                // 촬영 버튼
                CaptureButton(
                    onClick = {
                        if (!isCountingDown) {
                            if (timerSeconds > 0) {
                                countdownSeconds = timerSeconds
                                isCountingDown = true
                            } else {
                                cameraController.takePicture(onImageSaved, onError)
                            }
                        }
                    },
                    isCountingDown = isCountingDown
                )

                // 카메라 전환 버튼
                IconButton(
                    onClick = {
                        zoomLevel = 0f
                        cameraController.switchCamera(lifecycleOwner)
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .background(Color.White.copy(alpha = 0.2f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Cameraswitch,
                        contentDescription = "카메라 전환",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }

        // 찍은 사진 썸네일 (우측 하단)
        lastCapturedUri?.let { uri ->
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(bottom = 180.dp, end = 16.dp)
                    .size(80.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .border(2.dp, Color.White, RoundedCornerShape(12.dp))
                    .clickable {
                        val intent = Intent(Intent.ACTION_VIEW).apply {
                            setDataAndType(uri, "image/jpeg")
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        context.startActivity(intent)
                    }
            ) {
                AsyncImage(
                    model = uri,
                    contentDescription = "Last captured photo",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

/**
 * 촬영 버튼 Composable
 */
@Composable
private fun CaptureButton(
    onClick: () -> Unit,
    isCountingDown: Boolean = false,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.9f else 1f,
        label = "captureButtonScale"
    )

    val buttonColor = if (isCountingDown) Color.Red else Color.White

    Box(
        modifier = modifier
            .size(72.dp)
            .scale(scale)
            .clip(CircleShape)
            .background(buttonColor)
            .border(4.dp, buttonColor.copy(alpha = 0.5f), CircleShape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(60.dp)
                .clip(CircleShape)
                .background(buttonColor)
                .border(2.dp, Color.Gray.copy(alpha = 0.3f), CircleShape)
        )
    }
}