package jp.dip.utb.imoyokan

import android.annotation.TargetApi
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.text.format.DateFormat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import jp.dip.utb.imoyokan.futaba.getCatalogUrl
import java.util.*

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

fun createImoyokanIntent(context: Context, intent: Intent?): Intent {
    val result = Intent(context, NotificationReceiver::class.java)
        .putExtra(KEY_EXTRA_REQUEST_CODE, REQUEST_CODE_RELOAD_URL)
    if (intent != null) {
        result.putExtra(KEY_EXTRA_MAIL, intent.getStringExtra(KEY_EXTRA_MAIL))
    }
    return result
}

/** URL指定で再通知するIntent */
fun createNextPageIntent(context: Context, intent: Intent, requestCode: Int, url: String, vararg extras: Pair<String, Any>): PendingIntent {
    val newIntent = createImoyokanIntent(context, intent)
        .putExtra(KEY_EXTRA_URL, url)
        .putExtra(KEY_EXTRA_REQUEST_CODE, requestCode)
    extras.forEach {
        when (it.second) {
            is Int -> newIntent.putExtra(it.first, it.second as Int)
            is String -> newIntent.putExtra(it.first, it.second as String)
        }
    }
    return PendingIntent.getBroadcast(context, requestCode, newIntent, PendingIntent.FLAG_UPDATE_CURRENT)
}

/** URL指定で再通知するAction */
fun createNextPageAction(context: Context, intent: Intent, requestCode: Int, icon: Int, label: String, url: String): NotificationCompat.Action {
    return NotificationCompat.Action.Builder(
        icon,
        label,
        createNextPageIntent(context, intent, requestCode, url)
    ).build()
}

/**
 * カタログ
 */
fun createCatalogAction(context: Context, intent: Intent, requestCode: Int): NotificationCompat.Action {
    // カタログボタン
    val catalogIntent = createNextPageIntent(context, intent, requestCode, getCatalogUrl(intent.str(KEY_EXTRA_URL), Pref(context).catalog.sort))
    return NotificationCompat.Action.Builder(
        android.R.drawable.ic_menu_gallery,
        DateFormat.format("カタログ", Date()),
        catalogIntent
    ).build()
}

