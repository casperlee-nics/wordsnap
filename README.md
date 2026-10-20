# 拍照背單字（WordSnap）

Android 背單字 App。Kotlin + Jetpack Compose，單字存在手機裡（Room 資料庫）。

## 目前版本 0.1：單字本

- 列出所有單字（新的在上面）
- 右下角 + 新增單字與中文意思
- 垃圾桶圖示刪除單字

之後會陸續加入：拍照辨識單字、自動翻譯、翻卡複習、發音。

## 用 Android Studio 執行

1. 把 zip 解壓縮到一個資料夾，例如 `C:\Projects\WordSnap`。
2. 開啟 Android Studio，選 **Open**，選這個資料夾。
3. 第一次開啟會下載需要的元件（Gradle 同步），等右下角進度跑完，可能要幾分鐘。
   如果跳出「升級 Android Gradle Plugin」的提示，可以先按略過。
4. 手機設定 → 關於手機 → 連按「版本號碼」7 次，開啟開發人員選項；
   再到開發人員選項打開「USB 偵錯」。
5. 用 USB 線接上電腦，手機上按「允許」。
6. Android Studio 上方選到你的手機，按綠色 ▶ 執行。

遇到錯誤時，把 Android Studio 下方 Build 視窗的紅字貼給 Claude。
