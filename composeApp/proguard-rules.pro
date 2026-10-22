# Crashlytics: preserve file names and line numbers for readable stack traces.
-keepattributes SourceFile,LineNumberTable
-keepattributes *Annotation*,InnerClasses,Signature,EnclosingMethod

# Firebase (Android SDK) + GitLive Firebase KMP wrappers.
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }
-keep class dev.gitlive.firebase.** { *; }
-dontwarn com.google.firebase.**
-dontwarn dev.gitlive.firebase.**

# kotlinx.serialization: keep generated serializers and companion lookups.
-keepclassmembers class **$$serializer { *; }
-keepclasseswithmembers class * {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.linklabs.sorcerersandspirits.**$$serializer { *; }
-keepclassmembers class com.linklabs.sorcerersandspirits.** {
    *** Companion;
}
-dontnote kotlinx.serialization.**

# Ktor client + the coroutines it relies on.
-keep class io.ktor.** { *; }
-keepclassmembers class io.ktor.** { volatile <fields>; }
-keep class kotlinx.coroutines.** { *; }
-dontwarn io.ktor.**
-dontwarn kotlinx.coroutines.**

# Koin annotations / KSP-generated definitions.
-keep class org.koin.** { *; }
-keep @org.koin.core.annotation.* class * { *; }
-dontwarn org.koin.**
