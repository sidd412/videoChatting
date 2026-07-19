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
-keepattributes RuntimeVisibleAnnotations, RuntimeInvisibleAnnotations, Signature

# Keep generic signature of Call, Response (R8 full mode strips signatures from non-kept items)
-keep,allowobfuscation,allowshrinking interface retrofit2.Call
-keep,allowobfuscation,allowshrinking class retrofit2.Response

# Keep suspend function Continuations generic signatures
-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation

# ----------------------------------------------------
# GSON & Model Classes Rules (Keep fields for API parsing)
# ----------------------------------------------------
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}
-keep class com.videoChatting.echat.data.remote.model.** { *; }

# Keep all API model request, response, and Dto classes from being obfuscated or stripped
-keep class **.*Request { *; }
-keep class **.*Response { *; }
-keep class **.*Dto { *; }

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
