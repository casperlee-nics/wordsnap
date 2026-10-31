# 單字勇者的 R8 規則
# Room、ML Kit、WorkManager、Play 遊戲服務都有自帶規則，這裡只補自己程式需要的。

# 職業、道具等 enum 的名字會存進資料庫和存檔，保留名字不讓 R8 改掉
-keepclassmembers enum com.wordsnap.app.** {
    <fields>;
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# 資料庫的資料類別（Room 實體）保留欄位名稱，存檔搬家、雲端存檔比較保險
-keep class com.wordsnap.app.data.** { *; }

# 當機報告保留行號，Play Console 會自動用 mapping 檔還原
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
