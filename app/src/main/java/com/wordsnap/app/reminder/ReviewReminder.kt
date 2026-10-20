package com.wordsnap.app.reminder

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.wordsnap.app.MainActivity
import com.wordsnap.app.R
import com.wordsnap.app.data.AppDatabase
import java.util.Calendar
import java.util.concurrent.TimeUnit

private const val CHANNEL_ID = "review_reminder"
private const val WORK_NAME = "daily_review_reminder"
private const val REMIND_HOUR = 20 // 每天晚上 8 點提醒

object ReviewReminder {
    // 排定每天提醒一次；已經排過就不會重複
    fun schedule(context: Context) {
        val request = PeriodicWorkRequestBuilder<ReminderWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(millisUntilNextReminder(), TimeUnit.MILLISECONDS)
            .build()
        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
    }

    private fun millisUntilNextReminder(): Long {
        val now = Calendar.getInstance()
        val next = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, REMIND_HOUR)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (!after(now)) add(Calendar.DAY_OF_MONTH, 1)
        }
        return next.timeInMillis - now.timeInMillis
    }

    @SuppressLint("MissingPermission") // 已先用 areNotificationsEnabled() 檢查
    fun notify(context: Context, dueCount: Int) {
        val manager = NotificationManagerCompat.from(context)
        if (!manager.areNotificationsEnabled()) return

        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "每日複習提醒", NotificationManager.IMPORTANCE_DEFAULT)
        )
        val openApp = PendingIntent.getActivity(
            context, 0,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_word)
            .setContentTitle("該複習囉！")
            .setContentText("今天有 $dueCount 個字要複習")
            .setContentIntent(openApp)
            .setAutoCancel(true)
            .build()
        manager.notify(1, notification)
    }
}

// 時間到時，算一下有幾個字要複習，有的話就通知
class ReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val due = AppDatabase.get(applicationContext).wordDao().countDue(System.currentTimeMillis())
        if (due > 0) ReviewReminder.notify(applicationContext, due)
        return Result.success()
    }
}
