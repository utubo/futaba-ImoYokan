package jp.dip.utb.imoyokan

import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
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
        val builder = ImoyokanNotificationBuilder(context, intent)

        // まずはボタンとプログレスバーを表示する
        builder
            .addThreadAction(POSITION_KEEP)
            .addCatalogAction()
            .setProgress()
            .notifyThis()

        // 画像をダウンロードしてセット
        val bitmap = Picasso.get().load(intent.str(KEY_EXTRA_URL)).get()
        val view = RemoteViews(context.packageName, R.layout.notification_image)
        view.setImageViewBitmap(R.id.image, bitmap)

        // 共有ボタン
        view.setOnClickPendingIntent(R.id.share, builder.createShareUrlIntent(intent.str(KEY_EXTRA_IMAGE_SRC_URL)))

        // 表示するよ！
        builder
            .removeProgress()
            .setRemoteViews(view)
            .notifyThis()
    }

}