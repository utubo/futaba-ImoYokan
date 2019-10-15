package jp.dip.utb.imoyokan

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

        // スレッドボタンとカタログボタン
        var requestCode = REQUEST_CODE_RELOAD_URL
        notificationBuilder.addAction(createThreadAction(context, intent, ++requestCode))
        notificationBuilder.addAction(createCatalogAction(context, intent, ++requestCode))

        // まずはプログレスバーを表示する
        val notificationManager = NotificationManagerCompat.from(context)
        notificationBuilder
            .setProgress(0, 0, true /* = ずっとぐるぐるまわるスタイル */ )
            .notifySilent(context, CHANNEL_ID)

        // 画像をダウンロードしてセット
        val view = RemoteViews(context.packageName, R.layout.notification_image)
        val bitmap = Picasso.get().load(intent.str(KEY_EXTRA_URL)).get()
        view.setImageViewBitmap(R.id.image, bitmap)

        // 共有ボタン
        view.setOnClickPendingIntent(R.id.share, createShareUrlIntent(context, intent.str(KEY_EXTRA_IMAGE_SRC_URL)))

        // 表示するよ！
        notificationBuilder
            .removeProgress()
            .setRemoteViews(view)
        notificationManager.notify(0, notificationBuilder.build())
    }

}