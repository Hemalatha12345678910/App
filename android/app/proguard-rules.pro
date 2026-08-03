# Capacitor's native bridge invokes @PluginMethod-annotated methods via reflection from JS —
# without these, minification silently breaks Camera/Filesystem/Share at runtime with no compile error.
-keep class com.getcapacitor.** { *; }
-keep @com.getcapacitor.annotation.CapacitorPlugin class * { *; }
-keepclassmembers class * extends com.getcapacitor.Plugin {
    @com.getcapacitor.annotation.PermissionCallback <methods>;
    @com.getcapacitor.annotation.ActivityCallback <methods>;
    @com.getcapacitor.PluginMethod <methods>;
}
-keep class * extends com.getcapacitor.Plugin
-keepclassmembers class * implements com.getcapacitor.JSValueCastable { *; }

# This app hand-parses JSON via org.json (ApiClient.kt) using string-literal keys, not
# reflection/serialization, so no keep rules are needed there — safe under R8 as-is.

# Standard Kotlin coroutines/reflection metadata some libraries probe at runtime.
-keepattributes *Annotation*, InnerClasses, Signature, SourceFile, LineNumberTable
-keepclassmembers class kotlin.Metadata { *; }
-dontwarn kotlinx.coroutines.**
