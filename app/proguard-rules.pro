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

# --- Apache POI (đọc .docx) ---
# POI là lib desktop mang theo nhánh code không có trên Android
# (Saxon/XPath, AWT, StAX, OSGi...). App chỉ đọc XWPF nên:
#  - KHÔNG keep toàn bộ org.apache.poi (sẽ khiến R8 báo thiếu class + phình APK),
#  - chỉ keep phần XWPF + schema docx (nạp qua reflection),
#  - dontwarn các package desktop-only để R8 strip yên lặng.
-keep class org.apache.poi.xwpf.** { *; }
-keep class org.apache.poi.ooxml.** { *; }
-keep class org.apache.poi.openxml4j.** { *; }
-keep class org.apache.poi.util.** { *; }
-keep class org.openxmlformats.schemas.wordprocessingml.** { *; }
-keep class org.openxmlformats.schemas.officeDocument.** { *; }
-keep class org.openxmlformats.schemas.drawingml.** { *; }
-keep class schemaorg_apache_xmlbeans.** { *; }
-dontwarn org.apache.poi.**
-dontwarn org.openxmlformats.**
-dontwarn schemaorg_apache_xmlbeans.**
-dontwarn org.apache.xmlbeans.**
-dontwarn net.sf.saxon.**
-dontwarn javax.xml.stream.**
-dontwarn javax.annotation.**
-dontwarn java.awt.**
-dontwarn org.osgi.framework.**
-dontwarn aQute.bnd.annotation.spi.**
-dontwarn com.google.errorprone.annotations.**
-dontwarn com.graphbuilder.**
-dontwarn org.apache.logging.log4j.**

# PdfBox-Android: lib build riêng cho Android; giữ nguyên để parser chạy dưới R8.
# (Phase 7 đo size, nếu vượt 25MB sẽ thu hẹp keep này.)
-dontwarn com.tom_roush.**
-keep class com.tom_roush.pdfbox.** { *; }

# --- Retrofit + OkHttp (Phase 3: Gemini REST) ---
# Giữ signature generic để converter kotlinx.serialization hoạt động dưới R8.
-keepattributes Signature, InnerClasses, EnclosingMethod
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }
-keep interface com.study.flashcard.data.ai.GeminiApi { *; }
