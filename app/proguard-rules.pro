# Phase 1: ProGuard/R8 mặc định (dùng kèm proguard-android-optimize.txt).
# Phase 7 sẽ rà soát lại toàn bộ khi đo size APK.

# Room: giữ Entity/DAO/Database (debug đang bật minify nên cần ngay từ Phase 2).
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao class * { *; }
-keep class * implements android.os.Parcelable { *; }

# kotlinx.serialization: giữ annotation + serializer sinh tự động.
-keepattributes *Annotation*, InnerClasses, Signature, EnclosingMethod
-keepnames @kotlinx.serialization.Serializable class * { *; }
# An toàn thêm: giữ nguyên package data (Entity/DTO) khỏi obfuscation —
# package nhỏ, tốn size không đáng kể, tránh lỗi reflection dưới R8.
-keep class com.study.flashcard.data.** { *; }

# Apache POI + PdfBox-Android: nhiều class nạp qua reflection,
# giữ nguyên package để parser chạy được dưới R8 (đánh đổi size, đo ở Phase 7).
-dontwarn org.apache.poi.**
-dontwarn com.tom_roush.**
-dontwarn org.openxmlformats.**
-dontwarn schemaorg_apache_xmlbeans.**
-keep class org.apache.poi.** { *; }
-keep class org.openxmlformats.** { *; }
-keep class schemaorg_apache_xmlbeans.** { *; }
-keep class com.tom_roush.pdfbox.** { *; }
