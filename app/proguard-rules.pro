# LYNKO ProGuard Rules

# Keep annotation attributes
-keepattributes *Annotation*

# Keep Kotlin metadata
-keep class kotlin.Metadata { *; }

# Keep Firebase classes
-keep class com.google.firebase.** { *; }

# Keep application model classes
-keep class com.lynko.app.** { *; }
