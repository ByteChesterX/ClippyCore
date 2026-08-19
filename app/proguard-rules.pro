# ProGuard kuralları - ClippyCore

# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# Kotlin Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}

# Google Play Billing
-keep class com.android.billingclient.api.** { *; }
-keep class com.android.vending.billing.** { *; }

# DataStore
-keep class androidx.datastore.core.** { *; }
-keep class androidx.datastore.preferences.** { *; }

# Kotlin
-keep class kotlin.** { *; }
-keep class kotlin.Metadata { *; }
-dontwarn kotlin.**
-keepclassmembers class **$WhenMappings {
    <fields>;
}
-keepclassmembers class kotlin.Metadata {
    public <methods>;
}

# AndroidX
-keep class androidx.lifecycle.** { *; }
-keep class androidx.compose.** { *; }
-keep class androidx.navigation.** { *; }

# Entity sınıfları (Room için)
-keep class com.clippycore.app.data.database.ClipboardItem { *; }
-keep class com.clippycore.app.data.database.ClipboardItem$ItemType { *; }
