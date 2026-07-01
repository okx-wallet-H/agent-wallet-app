# Ktor
-keep class io.ktor.** { *; }
-dontwarn io.ktor.**

# Kotlinx Serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class kotlinx.serialization.json.** { kotlinx.serialization.KSerializer serializer(...); }
-keep,includedescriptorclasses class com.agentwallet.**$$serializer { *; }
-keepclassmembers class com.agentwallet.** { *** Companion; }
-keepclasseswithmembers class com.agentwallet.** { kotlinx.serialization.KSerializer serializer(...); }
