# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in C:\Users\admin\AppData\Local\Android\sdk/tools/proguard/proguard-android.txt
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
#-libraryjars            lib/aws-android-sdk-cognito-2.2.2.jar
#-libraryjars            lib/aws-android-sdk-core-2.2.2.jar
#-libraryjars            lib/aws-android-sdk-s3-2.2.2.jar
#Optimization options
-optimizationpasses 4
#Obfuscation options
-overloadaggressively
-keepattributes *Annotation*,EnclosingMethod,Signature
-keepnames class com.fasterxml.jackson.** { *; }
-dontwarn com.fasterxml.jackson.databind.**
-keep public class pl.droidsonroids.gif.GifIOException{<init>(int);}
-keep class pl.droidsonroids.gif.GifInfoHandle{<init>(long,int,int,int);}

-keepclassmembers class ** {
    public void onEvent*(***);
}
-keep public class com.pulseapp.** {*;}
-keep class com.daimajia.easing.** { *; }
-keep interface com.daimajia.easing.** { *; }
-dontwarn sun.misc.Unsafe
-dontwarn com.google.common.**
-keep class com.squareup.okhttp.** { *; }
-keep interface com.squareup.okhttp.** { *; }
-dontwarn okio.**
-dontwarn com.squareup.okhttp.*
-keepattributes Signature
-keepattributes *Annotation*
-keep public class com.google.android.gms.* { public *; }
-dontwarn com.google.android.gms.**
-keepclasseswithmembernames class * {
    native <methods>;
}
-keep class com.github.nkzawa.** { *; }
-keep interface com.github.nkzawa.** { *; }
 
-keep class android.support.v8.renderscript.** { *; }


-dontwarn com.amazonaws.**
-keep class com.amazonaws.** {*;}
-keep class com.google.api.services.youtube.** {*;}
-keep class com.google.api.client.** {*;}

#ACRA specifics
# Restore some Source file names and restore approximate line numbers in the stack traces,
# otherwise the stack traces are pretty useless
-keepattributes SourceFile,LineNumberTable
# Keep all the ACRA classes
-keep class org.acra.** { *; }

-keep class android.support.v4.** { *; }
-keep interface android.support.v4.app.** { *; }

#-dontoptimize

# Add this global rule
-keepattributes Signature

# This rule will properly ProGuard all the model classes in
# the package com.yourcompany.models. Modify to fit the structure
# of your app.
-keepclassmembers class com.pulseapp.android.models.** {
  *;
}

-keepclassmembers class com.pulseapp.android.modelView.** {
  *;
}