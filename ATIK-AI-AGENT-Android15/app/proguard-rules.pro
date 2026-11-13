-keepattributes *Annotation*
-keepclassmembers class * {
    @androidx.annotation.Keep <methods>;
}
-keep class com.atik.aiagent.data.model.** { *; }
-dontwarn com.google.firebase.ai.**
