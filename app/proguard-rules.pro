# ProGuard rules per StoppAI release build
# Preserva le classi critiche per Room, Kotlin reflection, Firebase, coroutines, HTTP

# Crash report piu' leggibili (mantieni line numbers)
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# =====================================================
# KOTLIN
# =====================================================
-keep class kotlin.Metadata { *; }
-keep class kotlin.reflect.** { *; }
-keepclassmembers class **$Companion { *; }

# Kotlinx coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}

# =====================================================
# ROOM DATABASE
# =====================================================
-keep class androidx.room.** { *; }
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao class * { *; }
-keepclassmembers @androidx.room.Entity class * { *; }
-keepclassmembers class * {
    @androidx.room.* <methods>;
    @androidx.room.* <fields>;
}

# Entity StoppAI
-keep class com.ifs.stoppai.db.** { *; }

# =====================================================
# FIREBASE
# =====================================================
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.firebase.**
-dontwarn com.google.android.gms.**

# =====================================================
# CLASSI STOPPAI (core/ui mantenute per riflessione e fragment transactions)
# =====================================================
-keep class com.ifs.stoppai.** { *; }
-keepnames class com.ifs.stoppai.ui.** { *; }
-keepnames class com.ifs.stoppai.core.** { *; }

# =====================================================
# ANDROID COMPONENTS
# =====================================================
-keep public class * extends android.app.Activity
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends androidx.fragment.app.Fragment
-keep public class * extends android.telecom.CallScreeningService

-keepclassmembers class * extends android.app.Activity {
    public void *(android.view.View);
}

# Enum values/valueOf
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Parcelable
-keepclassmembers class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator CREATOR;
}

# Serializable
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

# =====================================================
# JSON / HTTP
# =====================================================
-keep class org.json.** { *; }

# =====================================================
# BETTER-SQLITE / Native interop
# =====================================================
-keepclasseswithmembernames class * {
    native <methods>;
}

# =====================================================
# LOGGING (rimuovi Log.d/Log.v in release per perf)
# =====================================================
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
}

# Warnings da silenziare (dipendenze esterne)
-dontwarn org.jetbrains.annotations.**
-dontwarn kotlin.Unit
