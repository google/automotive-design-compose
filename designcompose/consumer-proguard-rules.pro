# Keep classes, constructors, methods, and fields accessed across JNI by libdc_jni
-keep class com.android.designcompose.FetchException { <init>(...); }
-keep class * extends com.android.designcompose.FetchException { <init>(...); }
-keep class com.android.designcompose.Jni { *; }
-keep class com.android.designcompose.TextSize { *; }
-keep class com.android.designcompose.DesignTextMeasure { *; }
-keep class com.android.designcompose.ProxyConfig { *; }
-keep class com.android.designcompose.HttpProxyConfig { *; }
