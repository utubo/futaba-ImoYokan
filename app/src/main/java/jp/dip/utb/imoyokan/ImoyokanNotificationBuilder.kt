package jp.dip.utb.imoyokan

import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.Icon
import android.net.Uri
import android.os.Build
import android.service.notification.StatusBarNotification
import android.util.Log
import android.view.View
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.RemoteInput
import jp.dip.utb.imoyokan.futaba.getCatalogUrl
import java.util.*
import kotlin.collections.HashMap

class ImoyokanNotificationBuilder(private val context: Context, private val intent: Intent) {

    private val pref = Pref.getInstance(context)
    private val builder = NotificationCompat.Builder(context, CHANNEL_ID)
    private val manager = NotificationManagerCompat.from(context)
    private var requestCode = REQUEST_CODE_RELOAD_URL_MIN
    private var isChannelReady = false
    private val uslShareIntents = HashMap<String, PendingIntent>()

    init {
        builder.setSmallIcon(R.drawable.ic_stat_imoyokan)
        if (pref.debugMode) {
            builder.color = Color.YELLOW
        }
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
        expandStatusBar()
    }

    private fun expandStatusBar() {
        try {
            @SuppressLint("WrongConstant")
            val service = context.getSystemService("statusbar")
            val clazz = Class.forName("android.app.StatusBarManager")
            val method = clazz.getMethod("expandNotificationsPanel")
            method.invoke(service)
        } catch (e: Throwable) {
            // ダメならログを出力して諦める
            Log.d(NOTIFY_NAME, "ステータスバーの展開に失敗", e)
        }
    }

    fun notifyMessage(title: String, message: String?) {
        builder
            .setContentTitle(title)
            .setContentText(message ?: "")
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
            .setCustomContentView(contentView)
            .setCustomBigContentView(bigContentView ?: contentView)
        return this
    }

    fun addAction(action: NotificationCompat.Action): ImoyokanNotificationBuilder {
        builder.addAction(action)
        return this
    }

    fun addNextPageAction(icon: Int, label: CharSequence, url: String, vararg extras: Pair<String, Any>): ImoyokanNotificationBuilder {
        val action = NotificationCompat.Action.Builder(icon, label, createNextPageIntent(url, *extras)).build()
        builder.addAction(action)
        return this
    }

    fun createThreadIntent(position: Int, vararg extras: Pair<String, Any>): PendingIntent {
        return when (position) {
            POSITION_KEEP -> createNextPageIntent(pref.lastThreadUrl, *extras)
            else -> createNextPageIntent(pref.lastThreadUrl, KEY_EXTRA_POSITION to position, *extras)
        }
    }

    fun addThreadAction(position: Int): ImoyokanNotificationBuilder {
        val action = NotificationCompat.Action.Builder(
            R.drawable.ic_thread,
            "スレッド",
            createThreadIntent(position)).build()
        builder.addAction(action)
        return this
    }

    fun addCatalogAction(): ImoyokanNotificationBuilder {
        val url = pref.lastCatalogUrl.ifBlank { getCatalogUrl(pref.lastThreadUrl) }
        addNextPageAction(R.drawable.ic_catalog, "カタログ", url)
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
    fun createImoyokanIntent(): Intent {
        return Intent(context, NotificationReceiver::class.java)
            .putExtra(KEY_EXTRA_POSITION, intent.getIntExtra(KEY_EXTRA_POSITION, THREAD_BOTTOM))
            .putExtra(KEY_EXTRA_IMAGE_INDEX, intent.getIntExtra(KEY_EXTRA_IMAGE_INDEX, 0))
    }

    fun createViewImageIntent(index: Int? = null): PendingIntent {
        val newIntent = createImoyokanIntent()
            .putExtra(KEY_EXTRA_ACTION, INTENT_ACTION_VIEW_IMAGE)
        if (index != null) {
            newIntent.putExtra(KEY_EXTRA_IMAGE_INDEX, index)
        }
        return PendingIntent.getBroadcast(context, ++requestCode, newIntent, PendingIntent.FLAG_UPDATE_CURRENT)
    }

    fun createPendingIntent(vararg extras: Pair<String, Any>): PendingIntent {
        val newIntent = createImoyokanIntent()
            .putAll(*extras)
        return PendingIntent.getBroadcast(context, ++requestCode, newIntent, PendingIntent.FLAG_UPDATE_CURRENT)
    }

    /** URLを通知で開くIntent */
    fun createNextPageIntent(url: String, vararg extras: Pair<String, Any>): PendingIntent {
        return createPendingIntent(KEY_EXTRA_ACTION to INTENT_ACTION_RELOAD_URL, KEY_EXTRA_URL to url, *extras)
    }

    /** URLを共有するPendingIntent  */
    fun createShareUrlIntent(url: String): PendingIntent {
        return uslShareIntents[url] ?: PendingIntent.getActivity(
            context,
            ++requestCode,
            getShareUrlChooser(url),
            PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    private fun getShareUrlChooser(url: String): Intent {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        // ↑これだけだと既定のブラウザくらいしか表示されないので↓MATCH_ALLで検索してEXTRA_INITIAL_INTENTSに追加する
        val list = context.packageManager.queryIntentActivities(intent, PackageManager.MATCH_ALL)
        list.sortWith(ResolveInfo.DisplayNameComparator(context.packageManager))
        list.sortByDescending { it.preferredOrder } // ←意味ないかも…
        val intents = ArrayList<Intent>()
        for (it in list) {
            if (it.activityInfo.packageName == context.packageName) continue
            intents.add(Intent(intent).setClassName(it.activityInfo.packageName, it.activityInfo.name))
        }
        return Intent
            .createChooser(Intent(), url)
            .putExtra(Intent.EXTRA_INITIAL_INTENTS, intents.toTypedArray())
    }

    fun getLastNotification(): StatusBarNotification? {
        val sv = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        return sv.activeNotifications.firstOrNull { it.packageName == context.packageName }
    }

    fun addRemoteInput(icon: Int, label: String, placeHolder: String, key: String, intent: Intent) {
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_REPLY_MIN + Random().nextInt(10000), // 返信のrequestCodeはかぶらないようにする！,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT
        )
        val remoteInput = RemoteInput.Builder(key)
            .setLabel(placeHolder)
            .build()
        val action = NotificationCompat.Action
            .Builder(icon, label, pendingIntent)
            .addRemoteInput(remoteInput)
            .build()
        this.addAction(action)
    }

}

fun RemoteViews.setOnClickOrGone(id: Int, b: Boolean, f: () -> PendingIntent) {
    if (b) {
        this.setOnClickPendingIntent(id, f())
        this.setViewVisibility(id, View.VISIBLE)
    } else {
        this.setViewVisibility(id, View.GONE)
    }
}

fun RemoteViews.setOnClickOrInvisible(id: Int, b: Boolean, f: () -> PendingIntent) {
    if (b) {
        this.setOnClickPendingIntent(id, f())
        this.setViewVisibility(id, View.VISIBLE)
    } else {
        this.setViewVisibility(id, View.INVISIBLE)
    }
}

fun RemoteViews.setImageViewAny(id: Int, image: Any?) {
    if (image == null) {
        setImageViewBitmap(id, null)
        return
    }
    when (image) {
        is Bitmap -> setImageViewBitmap(id, image)
        is Int -> setImageViewResource(id, image)
        is Icon -> setImageViewIcon(id, image)
        is Uri -> setImageViewUri(id, image)
    }
}
