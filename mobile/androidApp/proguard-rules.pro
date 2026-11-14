# Rules for the release build; the libraries ship their own consumer rules and these cover the
# serializable classes of the application and the shared module (kotlinx.serialization README).
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class kotlinx.serialization.json.** { kotlinx.serialization.KSerializer serializer(...); }
-keep,includedescriptorclasses class com.manuelalejandro.delfos.**$$serializer { *; }
-keepclassmembers class com.manuelalejandro.delfos.** { *** Companion; }
-keepclasseswithmembers class com.manuelalejandro.delfos.** { kotlinx.serialization.KSerializer serializer(...); }
