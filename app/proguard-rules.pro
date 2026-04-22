# Add project specific ProGuard rules here.
-keep class org.nanohttpd.** { *; }
-keep class com.pocketphp.server.** { *; }
-keep class com.pocketphp.tunnel.** { *; }
-keepattributes *Annotation*
-keepattributes SourceFile,LineNumberTable
-dontwarn org.nanohttpd.**
