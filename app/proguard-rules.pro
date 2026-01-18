# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.kts.

# ============================================
# CameraX
# ============================================
-keep class androidx.camera.** { *; }

# ============================================
# Jetpack Compose
# ============================================
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**

# Keep Compose runtime
-keepclassmembers class * {
    @androidx.compose.runtime.Composable <methods>;
}

# ============================================
# Accompanist
# ============================================
-keep class com.google.accompanist.** { *; }
-dontwarn com.google.accompanist.**

# ============================================
# Coil (Image Loading)
# ============================================
-keep class coil.** { *; }
-dontwarn coil.**
-keep class io.coil.** { *; }
-dontwarn io.coil.**

# ============================================
# Kotlin
# ============================================
-keep class kotlin.** { *; }
-keep class kotlinx.** { *; }
-dontwarn kotlin.**
-dontwarn kotlinx.**

# Keep Kotlin Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}

# ============================================
# AndroidX
# ============================================
-keep class androidx.lifecycle.** { *; }
-keep class androidx.activity.** { *; }

# ============================================
# App specific rules
# ============================================
# Keep the main activity
-keep class com.silentcamera.MainActivity { *; }

# Keep camera related classes
-keep class com.silentcamera.camera.** { *; }

# ============================================
# General rules
# ============================================
# Keep annotations
-keepattributes *Annotation*

# Keep source file names for better crash reports
-keepattributes SourceFile,LineNumberTable

# Remove logging in release
-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
    public static int i(...);
}
