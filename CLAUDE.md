# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 프로젝트 개요

SilentCamera는 무음 촬영 기능을 제공하는 Android 카메라 앱입니다. Kotlin과 Jetpack Compose로 작성되었으며, CameraX 라이브러리를 사용합니다.

## 빌드 및 실행

```bash
# 빌드
./gradlew build

# Debug APK 빌드
./gradlew assembleDebug

# Release APK 빌드
./gradlew assembleRelease

# 연결된 기기에 설치 및 실행
./gradlew installDebug

# Lint 검사
./gradlew lint
```

## 기술 스택

- **Language**: Kotlin 1.9.20, Java 17
- **Min SDK**: 24 (Android 7.0)
- **Target SDK**: 34 (Android 14)
- **UI**: Jetpack Compose (Material 3, Compose BOM 2023.10.01)
- **Camera**: CameraX 1.3.0
- **Permissions**: Accompanist Permissions 0.32.0

## 아키텍처

### 패키지 구조

```
com.example.silentcamera/
├── MainActivity.kt          # 앱 진입점, 권한 처리
├── camera/
│   ├── CameraController.kt  # CameraX 카메라 제어 (줌, 전환, 촬영)
│   └── SilentCapture.kt     # 무음 촬영 로직 (AudioManager로 시스템 음소거)
└── ui/
    ├── CameraPreview.kt     # CameraX PreviewView의 Compose 래퍼
    ├── CameraScreen.kt      # 메인 카메라 UI (타이머, 줌 슬라이더, 촬영 버튼)
    └── theme/Theme.kt       # Material 3 테마 설정
```

### 핵심 흐름

1. **권한 처리**: `MainActivity`에서 Accompanist로 카메라 권한을 요청
2. **카메라 초기화**: `CameraController.startCamera()`가 CameraX Provider를 설정하고 Preview/ImageCapture를 바인딩
3. **무음 촬영**: `SilentCapture.takeSilentPhoto()`가 AudioManager로 STREAM_SYSTEM/STREAM_NOTIFICATION을 일시 음소거 후 촬영, 완료 시 해제
4. **저장**: MediaStore를 통해 `Pictures/SilentCamera/` 폴더에 JPEG 저장

### 주요 기능

- 전면/후면 카메라 전환
- 핀치 줌 (0~10x, 세로 슬라이더 UI)
- 타이머 촬영 (0, 2, 3, 5, 10, 15, 30초)
- 촬영 시 시스템 셔터음 음소거
