# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Preserve line numbers for debugging
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Keep annotations
-keepattributes *Annotation*,Signature,Exception

# Ignore missing compile-time annotations
-dontwarn com.google.errorprone.annotations.**
-dontwarn javax.annotation.**
-dontwarn javax.annotation.concurrent.**
-dontwarn javax.mail.**
-dontwarn javax.activation.**

# Keep JavaMail classes (used for email sending)
-keep class javax.mail.** { *; }
-keep class javax.activation.** { *; }
-keep class com.sun.mail.** { *; }

# Keep libphonenumber (used for phone number validation)
-keep class com.google.i18n.phonenumbers.** { *; }

# Keep app model classes
-keep class info.meuse24.smsforwarderneoA1.domain.model.** { *; }

# Keep ViewModels
-keep class * extends androidx.lifecycle.ViewModel {
    <init>(...);
}

# Keep Composable functions
-keep @androidx.compose.runtime.Composable class * { *; }

# Remove logging in release
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
}

# Timber logging library
-dontwarn org.jetbrains.annotations.**
-keep class timber.log.Timber { *; }
-keep class timber.log.Timber$Tree { *; }
-keep class timber.log.Timber$DebugTree { *; }

# Keep custom Timber trees
-keep class info.meuse24.smsforwarderneoA1.data.local.FileLoggingTree { *; }

# Strip debug logs in release builds
-assumenosideeffects class timber.log.Timber$Tree {
    public void v(...);
    public void d(...);
}

-assumenosideeffects class timber.log.Timber {
    public static *** v(...);
    public static *** d(...);
}