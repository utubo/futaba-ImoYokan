package jp.dip.utb.imoyokan

import android.annotation.TargetApi
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

/**
 * 音を鳴らさず通知する
 */
fun NotificationCompat.Builder.notifySilent(context: Context, channelId: String) {
    // 音がならないようにする
    this.setPriority(NotificationCompat.PRIORITY_LOW).build()
    val notificationManager = NotificationManagerCompat.from(context)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        // Android8以降はチャンネル登録が必要
        setupNotificationManager(notificationManager, channelId)
    }
    // 表示するよ!
    notificationManager.notify(0, this.build())
}

@TargetApi(Build.VERSION_CODES.O)
private fun setupNotificationManager(notificationManager: NotificationManagerCompat, channelId: String) {
    if (notificationManager.getNotificationChannel(channelId) == null) {
        val mChannel = NotificationChannel(CHANNEL_ID, NOTIFY_NAME, NotificationManager.IMPORTANCE_LOW)
        mChannel.apply {
            description = NOTIFY_DESCRIPTION
        }
        notificationManager.createNotificationChannel(mChannel)
    }
}

fun NotificationCompat.Builder.removeProgress(): NotificationCompat.Builder {
    this.setProgress(0, 0, false) // これでプログレスバーが消えるんだって
    return this
}

/**
 * URLを共有するIntent
 */
fun createShareUrlIntent(context: Context, url: String, target: Class<*>? = null): PendingIntent {
    val share = Intent(Intent.ACTION_VIEW, Uri.parse(url))
    if (target != null) {
        share.setClassName(target.`package`!!.name, target.name)
    }
    return PendingIntent.getActivity(
        context,
        REQUEST_CODE_SHARE,
        Intent.createChooser(share, url),
        PendingIntent.FLAG_UPDATE_CURRENT
    )
}
