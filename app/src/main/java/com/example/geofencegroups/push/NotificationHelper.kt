package com.example.geofencegroups.push

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.geofencegroups.permissions.Perms

object NotificationHelper {
    const val CHANNEL_ID = "member_events"

    fun createChannel(ctx: Context) {
        if (Build.VERSION.SDK_INT < 26) return
        val ch = NotificationChannel(CHANNEL_ID, "Member geofence events", NotificationManager.IMPORTANCE_HIGH)
        ctx.getSystemService(NotificationManager::class.java).createNotificationChannel(ch)
    }

    @SuppressLint("MissingPermission")
    fun show(ctx: Context, title: String?, body: String?) {
        if (!Perms.notifications(ctx)) return
        val n = NotificationCompat.Builder(ctx, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title).setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH).setAutoCancel(true).build()
        NotificationManagerCompat.from(ctx).notify(System.currentTimeMillis().toInt(), n)
    }
}
