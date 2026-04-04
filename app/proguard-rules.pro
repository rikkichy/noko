-keepattributes Signature
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}
-dontwarn retrofit2.**
-dontwarn okhttp3.internal.platform.**

-keepattributes *Annotation*, InnerClasses
-keep,includedescriptorclasses class cat.ri.noko.model.**$$serializer { *; }
-keepclassmembers class cat.ri.noko.model.** {
    *** Companion;
}
-keepclasseswithmembers class cat.ri.noko.model.** {
    kotlinx.serialization.KSerializer serializer(...);
}

-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**
