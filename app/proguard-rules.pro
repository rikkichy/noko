# Add project specific ProGuard rules here.

# ── Retrofit ──────────────────────────────────────────────
-keepattributes Signature
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}
-dontwarn retrofit2.**
-dontwarn okhttp3.internal.platform.**

# ── kotlinx-serialization ─────────────────────────────────
-keepattributes *Annotation*, InnerClasses
-keep,includedescriptorclasses class cat.ri.noko.model.**$$serializer { *; }
-keepclassmembers class cat.ri.noko.model.** {
    *** Companion;
}
-keepclasseswithmembers class cat.ri.noko.model.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# ── OkHttp ────────────────────────────────────────────────
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**
