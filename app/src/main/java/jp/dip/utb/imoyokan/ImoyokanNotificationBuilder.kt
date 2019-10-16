package jp.dip.utb.imoyokan

import android.annotation.TargetApi
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import jp.dip.utb.imoyokan.futaba.getCatalogUrl

class ImoyokanNotificationBuilder(private val context: Context, private val intent: Intent) {

    private val pref = Pref.getInstance(context)
    private val builder = NotificationCompat.Builder(context, CHANNEL_ID)
    private val manager = NotificationManagerCompat.from(context)
    private var requestCode = REQUEST_CODE_RELOAD_URL_MIN
    private var isChannelReady = false

    init {
        builder.setSmallIcon(R.drawable.ic_stat_imoyokan)
    }

    fun notifyThis() {
        if (!isChannelReady) {
            // 音がならないようにする
            builder.setPriority(NotificationCompat.PRIORITY_LOW).build()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // Android8以降はチャンネル登録が必要
                setupNotificationManager()
            }
            isChannelReady = true
        }
        // 表示するよ!
        manager.notify(0, builder.build())
    }

    fun notifyMessage(title: String, message: String) {
        builder
            .setContentTitle(title)
            .setContentText(message)
        removeProgress()
        notifyThis()
    }

    @TargetApi(Build.VERSION_CODES.O)
    private fun setupNotificationManager() {
        if (manager.getNotificationChannel(CHANNEL_ID) == null) {
            val mChannel = NotificationChannel(CHANNEL_ID, NOTIFY_NAME, NotificationManager.IMPORTANCE_LOW)
            mChannel.apply {
                description = NOTIFY_DESCRIPTION
            }
            manager.createNotificationChannel(mChannel)
        }
    }

    fun setRemoteViews(contentView: RemoteViews, bigContentView: RemoteViews? = null): ImoyokanNotificationBuilder {
        builder.setStyle(NotificationCompat.DecoratedCustomViewStyle())
            .setCustomBigContentView(contentView)
            .setCustomContentView(bigContentView ?: contentView)
        return this
    }

    fun addAction(action: NotificationCompat.Action): ImoyokanNotificationBuilder {
        builder.addAction(action)
        return this
    }

    fun addNextPageAction(icon: Int, label: CharSequence, url: String): ImoyokanNotificationBuilder {
        val action = NotificationCompat.Action.Builder(icon, label, createNextPageIntent(url)).build()
        builder.addAction(action)
        return this
    }

    fun addThreadAction(): ImoyokanNotificationBuilder {
        addNextPageAction(android.R.drawable.ic_menu_close_clear_cancel, "スレッド", pref.lastThreadUrl)
        return this
    }

    fun addCatalogAction(): ImoyokanNotificationBuilder {
        val url = pref.lastCatalogUrl.blankToNull() ?: getCatalogUrl(pref.lastThreadUrl)
        addNextPageAction(android.R.drawable.ic_menu_gallery, "カタログ", url)
        return this
    }

    fun setProgress(): ImoyokanNotificationBuilder {
        builder.setProgress(0, 0, true) // ずっとぐるぐるまわるスタイル
        return this
    }

    fun removeProgress(): ImoyokanNotificationBuilder {
        builder.setProgress(0, 0, false) // これでプログレスバーが消えるんだって
        return this
    }

    /** 設定に保存するまでもないIntentで引きずり回すパラメータをセットしたIntent */
    fun createImoyokanIntent(context: Context, intent: Intent): Intent {
        return Intent(context, NotificationReceiver::class.java)
            .putExtra(KEY_EXTRA_MAIL, intent.getStringExtra(KEY_EXTRA_MAIL)) // 今の所メアドくらいか…
    }

    /** URLを通知で開くIntent */
    fun createNextPageIntent(url: String, vararg extras: Pair<String, Any>): PendingIntent {
        val newIntent = createImoyokanIntent(context, intent)
            .putExtra(KEY_EXTRA_ACTION, INTENT_ACTION_RELOAD_URL)
            .putExtra(KEY_EXTRA_URL, url)
        extras.forEach {
            when (it.second) {
                is Int -> newIntent.putExtra(it.first, it.second as Int)
                is String -> newIntent.putExtra(it.first, it.second as String)
            }
        }
        return PendingIntent.getBroadcast(context, ++requestCode, newIntent, PendingIntent.FLAG_UPDATE_CURRENT)
    }

    /** URLを共有するIntent  */
    fun createShareUrlIntent(url: String, target: Class<*>? = null): PendingIntent {
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

}
