# Proguard rules for eChat

# ----------------------------------------------------
# General Android Rules
# ----------------------------------------------------
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod

# ----------------------------------------------------
# Retrofit & OkHttp Rules
# ----------------------------------------------------
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }
-keepattributes RuntimeVisibleAnnotations, RuntimeInvisibleAnnotations

# ----------------------------------------------------
# GSON & Model Classes Rules (Keep fields for API parsing)
# ----------------------------------------------------
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}
-keep class com.videoChatting.echat.data.remote.model.** { *; }

# ----------------------------------------------------
# Agora Video RTC Rules
# ----------------------------------------------------
-keep class io.agora.** { *; }
-dontwarn io.agora.**

# ----------------------------------------------------
# Socket.io Rules
# ----------------------------------------------------
-dontwarn io.socket.**
-keep class io.socket.** { *; }
-dontwarn okio.**

# ----------------------------------------------------
# Firebase Rules
# ----------------------------------------------------
-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**

# ----------------------------------------------------
# Dagger Hilt Rules
# ----------------------------------------------------
-keep class dagger.hilt.** { *; }
-dontwarn dagger.hilt.**
