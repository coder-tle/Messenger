# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in /home/chris/Android/Sdk/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Add any project specific keep options here:

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

-keep class com.android.okhttp.internal.** { *; }
-keep class android.** { *; }
-keep class com.android.application.** { *; }
-keep class com.android.support.** { *; }
-keep class com.android.tools.** { *; }
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }
-keep class com.github.bettehem.** { *; }
-keep class org.jetbrains.kotlin.** { *; }
-keep class org.apache.http.** { *; }
-keep class org.json.** { *; }
-keep class com.rockerhieu.emojicon.** { *; }
-keep class com.google.api.client.** { *; }
-keep class org.apache.legacy.** { *; }
-keep class de.hdodenhof.** { *; }
-keep class com.google.gms.** { *; }
-keep class org.apache.commons.codec.android.binary.** { *; }
-keep class javax.crypto.** { *; }
-keep class java.security.** { *; }
-dontwarn **.**

