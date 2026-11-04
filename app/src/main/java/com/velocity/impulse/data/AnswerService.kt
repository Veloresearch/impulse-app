package com.velocity.impulse.data

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.velocity.impulse.MainActivity
import com.velocity.impulse.R
import com.velocity.impulse.ui.Lang

/**
 * Keeps the app alive while answers are written, so one finishes with Impulse in the background.
 * [AppModel] tells it how many chats are writing; at zero it goes.
 */
class AnswerService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        channels(this)
        val n = writing
        val note = NotificationCompat.Builder(this, WRITING)
            .setSmallIcon(R.drawable.notify)
            .setContentTitle(if (n > 1) Lang.tr("Writing $n answers", "Piszę $n odpowiedzi") else Lang.tr("Writing an answer", "Piszę odpowiedź"))
            .setContentText(Lang.tr("It finishes even if you leave Impulse.", "Skończy się, nawet gdy wyjdziesz z Impulse."))
            .setOngoing(true)
            .setSilent(true)
            .setContentIntent(opening(this, null))
            .build()
        // startForeground comes first even when nothing is left: stopping before it crashes on older Androids.
        ServiceCompat.startForeground(this, ONGOING, note, if (Build.VERSION.SDK_INT >= 29) ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC else 0)
        started = true
        if (n == 0) stopSelf()
        return START_NOT_STICKY
    }

    override fun onTimeout(startId: Int, fgsType: Int) = stopSelf()

    override fun onDestroy() {
        started = false
        super.onDestroy()
    }

    companion object {
        /** Opens the chat named by this extra. */
        const val CHAT = "com.velocity.impulse.CHAT"
        private const val WRITING = "writing"
        private const val ANSWERS = "answers"
        private const val ONGOING = 1

        @Volatile
        private var writing = 0

        /** The service reached startForeground; stopping it before then crashes the app. */
        @Volatile
        private var started = false

        /** Runs the service while [count] chats are writing and stops it at zero. */
        fun keep(ctx: Context, count: Int) {
            val was = writing
            writing = count
            val intent = Intent(ctx, AnswerService::class.java)
            if (count == 0) {
                // Not started yet: onStartCommand sees zero and stops itself after startForeground.
                if (was > 0 && started) ctx.stopService(intent)
                return
            }
            if (count == was) return
            // Android 12+ refuses a start from the background; the answer then runs without the service.
            runCatching { ContextCompat.startForegroundService(ctx, intent) }
        }

        /** Says an answer is ready (or failed); tapping it opens [chatId]. */
        fun done(ctx: Context, chatId: String, title: String, text: String, failed: Boolean) {
            val nm = NotificationManagerCompat.from(ctx)
            if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return
            if (!nm.areNotificationsEnabled()) return
            channels(ctx)
            val note = NotificationCompat.Builder(ctx, ANSWERS)
                .setSmallIcon(R.drawable.notify)
                .setContentTitle(title.ifBlank { if (failed) Lang.tr("The answer failed", "Odpowiedź się nie udała") else Lang.tr("Your answer is ready", "Odpowiedź gotowa") })
                .setContentText(text)
                .setStyle(NotificationCompat.BigTextStyle().bigText(text))
                .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                .setAutoCancel(true)
                .setContentIntent(opening(ctx, chatId))
                .build()
            runCatching { nm.notify(chatId.hashCode(), note) }
        }

        fun dismiss(ctx: Context, chatId: String) = NotificationManagerCompat.from(ctx).cancel(chatId.hashCode())

        private fun opening(ctx: Context, chatId: String?): PendingIntent {
            val i = Intent(ctx, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            if (chatId != null) i.putExtra(CHAT, chatId)
            return PendingIntent.getActivity(ctx, chatId?.hashCode() ?: 0, i, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        }

        private fun channels(ctx: Context) {
            ctx.getSystemService(NotificationManager::class.java).createNotificationChannels(
                listOf(
                    NotificationChannel(ANSWERS, Lang.tr("Answers", "Odpowiedzi"), NotificationManager.IMPORTANCE_DEFAULT),
                    NotificationChannel(WRITING, Lang.tr("Writing", "Pisanie"), NotificationManager.IMPORTANCE_LOW),
                ),
            )
        }
    }
}
