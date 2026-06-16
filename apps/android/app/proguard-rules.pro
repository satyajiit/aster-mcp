# Aster ProGuard Rules

# Keep Kotlin serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}

-keep,includedescriptorclasses class com.aster.**$$serializer { *; }
-keepclassmembers class com.aster.** {
    *** Companion;
}
-keepclasseswithmembers class com.aster.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
-keepnames class okhttp3.internal.publicsuffix.PublicSuffixDatabase

# Hilt
-keepclasseswithmembers class * {
    @dagger.hilt.* <methods>;
}

# Ktor
-dontwarn io.ktor.**
-keep class io.ktor.** { *; }
-keepclassmembers class io.ktor.** { *; }

# MCP SDK
-dontwarn io.modelcontextprotocol.**
-keep class io.modelcontextprotocol.** { *; }

# ML Kit (on-device OCR). Bundled consumer-proguard handles the model asset under
# resource shrinking; explicit keep/dontwarn matches the per-dep pattern above.
-keep class com.google.mlkit.** { *; }
-dontwarn com.google.mlkit.**

# AIDL interfaces
-keep class com.aster.ipc.** { *; }
