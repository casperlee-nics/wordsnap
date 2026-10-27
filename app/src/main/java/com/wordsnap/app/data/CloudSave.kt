package com.wordsnap.app.data

import android.app.Activity
import android.content.Context
import com.google.android.gms.games.PlayGames
import com.google.android.gms.games.PlayGamesSdk
import com.google.android.gms.games.SnapshotsClient
import com.google.android.gms.games.snapshot.SnapshotMetadataChange
import com.wordsnap.app.R
import kotlinx.coroutines.tasks.await

// 雲端存檔：用 Google Play 遊戲的「已儲存的遊戲」功能，存檔跟著玩家的 Google 帳號走
// 要先在 Play 管理中心開好 Play 遊戲服務，把專案 ID 填進 res/values/games-ids.xml，這裡才會開放
object CloudSave {
    // 每位玩家在雲端只放一份存檔
    private const val SNAPSHOT_NAME = "wordsnap-save"

    class CloudInfo(val savedAt: Long, val description: String)

    fun isConfigured(context: Context): Boolean =
        context.getString(R.string.game_services_project_id).isNotBlank()

    // App 啟動時呼叫一次
    fun init(context: Context) {
        if (isConfigured(context)) PlayGamesSdk.initialize(context.applicationContext)
    }

    // 已經登入就直接回傳 true；沒有的話跳出 Play 遊戲的登入畫面
    suspend fun signIn(activity: Activity): Boolean {
        val client = PlayGames.getGamesSignInClient(activity)
        if (client.isAuthenticated().await().isAuthenticated) return true
        return client.signIn().await().isAuthenticated
    }

    // 雲端上那份存檔是什麼時候存的；還沒存過回傳 null
    suspend fun info(activity: Activity): CloudInfo? {
        val buffer = PlayGames.getSnapshotsClient(activity).load(true).await().get() ?: return null
        try {
            val meta = buffer.firstOrNull { it.uniqueName == SNAPSHOT_NAME } ?: return null
            return CloudInfo(meta.lastModifiedTimestamp, meta.description.orEmpty())
        } finally {
            buffer.release()
        }
    }

    // 上傳；存檔太大（超過 Play 遊戲的上限）回傳 false
    suspend fun upload(activity: Activity, bytes: ByteArray, description: String): Boolean {
        val client = PlayGames.getSnapshotsClient(activity)
        if (bytes.size > client.getMaxDataSize().await()) return false
        val snapshot = client.open(SNAPSHOT_NAME, true, SnapshotsClient.RESOLUTION_POLICY_MOST_RECENTLY_MODIFIED)
            .await().data ?: return false
        if (!snapshot.snapshotContents.writeBytes(bytes)) {
            client.discardAndClose(snapshot).await()
            return false
        }
        val change = SnapshotMetadataChange.Builder().setDescription(description).build()
        client.commitAndClose(snapshot, change).await()
        return true
    }

    // 下載雲端存檔；還沒存過回傳 null
    suspend fun download(activity: Activity): ByteArray? {
        if (info(activity) == null) return null
        val client = PlayGames.getSnapshotsClient(activity)
        val snapshot = client.open(SNAPSHOT_NAME, false, SnapshotsClient.RESOLUTION_POLICY_MOST_RECENTLY_MODIFIED)
            .await().data ?: return null
        return try {
            snapshot.snapshotContents.readFully()
        } finally {
            client.discardAndClose(snapshot).await()
        }
    }
}
