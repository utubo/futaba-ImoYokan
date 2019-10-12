package jp.dip.utb.imoyokan

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.squareup.picasso.Picasso
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class ImageNotification(private val context: Context, private val intent: Intent) {

    fun notifyThis() {
        GlobalScope.launch {
            notifyAsync()
        }
    }

    private fun notifyAsync() {
        val notificationBuilder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_imoyokan)

        // アクションボタン
        var requestCode = REQUEST_CODE_RELOAD
        val threadUrl = intent.str(KEY_EXTRA_BACK_URL)
        if (threadUrl.isNotEmpty()) {
            val clickIntent = Intent(context, NotificationReceiver::class.java)
                .putExtra(KEY_EXTRA_REQUEST_CODE, REQUEST_CODE_RELOAD)
                .putExtra(KEY_EXTRA_SORT, intent.str(KEY_EXTRA_SORT))
                .putExtra(Intent.EXTRA_TEXT, threadUrl)
            notificationBuilder.addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                "スレッド",
                PendingIntent.getBroadcast(context, ++requestCode, clickIntent, PendingIntent.FLAG_CANCEL_CURRENT)
            )
        }

        // まずはプログレスバーを表示する
        val notificationManager = NotificationManagerCompat.from(context)
        notificationBuilder
            .setProgress(0, 0, true /* = ずっとぐるぐるまわるスタイル */ )
            .notifySilent(context, CHANNEL_ID)

        // 画像をダウンロードしてセット
        val view = RemoteViews(context.packageName, R.layout.notification_image)
        val bitmap = Picasso.get().load(intent.str(Intent.EXTRA_TEXT)).get()
        view.setImageViewBitmap(R.id.image, bitmap)

        // 共有ボタン
        view.setOnClickPendingIntent(R.id.share, createShareUrlIntent(context, intent.str(KEY_EXTRA_IMAGE_SRC_URL)))

        // 表示するよ！
        notificationBuilder
            .removeProgress()
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
            .setContent(view)
            .setCustomBigContentView(view)
        notificationManager.notify(0, notificationBuilder.build())
    }

}