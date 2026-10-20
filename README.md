# 拍照背單字（WordSnap）

Android 背單字 App。Kotlin + Jetpack Compose，單字存在手機裡（Room 資料庫）。

## 目前版本 0.2：拍照找單字

- 單字本：列出所有單字（新的在上面），垃圾桶圖示刪除
- 右下角「拍照」：拍照或從相簿選圖，App 找出裡面的英文單字，點選後加入單字本（已有的字會略過）
- 右下角 +：手動新增單字
- 點一下單字：編輯英文或中文意思

之後會陸續加入：自動翻譯、翻卡複習、發音。

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
