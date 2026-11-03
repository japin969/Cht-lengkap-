# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.kts.

# Preserve source file & line numbers for readable crash stack traces via ErrorHandler/Timber
-keepattributes SourceFile,LineNumberTable
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod

# 1. Jayra Data & Domain Models (Firestore POJO serialization & Moshi DTOs)
-keep class com.example.model.** { *; }
-keep class com.example.data.** { *; }

# 2. WebRTC (JNI / native reflection callbacks if present)
-keep class org.webrtc.** { *; }
-dontwarn org.webrtc.**

# 3. Moshi & Retrofit JSON Serialization
-keep class com.squareup.moshi.** { *; }
-keepclasseswithmembers class * {
    @com.squareup.moshi.* <methods>;
}
-keep @com.squareup.moshi.JsonClass class * { *; }
-dontwarn com.squareup.moshi.**

# 4. Firebase (Auth, Firestore, Messaging, AI Logic, App Check)
-keep class com.google.firebase.** { *; }
-keepnames class * implements java.io.Serializable
-keepclassmembers class * {
    @com.google.firebase.firestore.PropertyName <fields>;
    @com.google.firebase.firestore.PropertyName <methods>;
}

# 5. Kotlin Metadata & Coroutines
-keep class kotlin.Metadata { *; }
-dontwarn kotlinx.coroutines.**
