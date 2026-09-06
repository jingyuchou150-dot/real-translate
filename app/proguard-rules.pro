# 保留 Vosk / Kaldi 的 native 方法与模型加载逻辑
-keep class org.vosk.** { *; }
-keepclassmembers class org.vosk.** { *; }

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
