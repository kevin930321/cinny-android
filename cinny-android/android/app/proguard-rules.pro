# Keep JNA & UniFFI generated Kotlin bindings intact in release R8/ProGuard builds
-keep class com.sun.jna.** { *; }
-keep class * implements com.sun.jna.** { *; }
-dontwarn java.awt.*

-keep class uniffi.cinny_core.** { *; }
-keepclassmembers class uniffi.cinny_core.** { *; }

# Keep Kotlin Coroutines & lifecycle classes
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
