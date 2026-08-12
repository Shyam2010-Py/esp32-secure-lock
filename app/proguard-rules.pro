# Proguard rules
-keepattributes *Annotation*
-keepclassmembers class * {
    @androidx.annotation.Keep <methods>;
}
