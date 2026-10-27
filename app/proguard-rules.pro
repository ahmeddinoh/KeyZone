# CipherVault ProGuard Rules

# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# Kotlin Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# Keep crypto classes
-keep class com.ciphervault.app.crypto.** { *; }
-keep class com.ciphervault.app.data.db.** { *; }
