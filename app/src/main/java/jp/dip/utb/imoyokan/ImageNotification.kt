package jp.dip.utb.imoyokan

import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import jp.dip.utb.imoyokan.futaba.toThumbnailUrl
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

        // スレッド情報を取得
        val threadInfo = Cache(context).loadFromCache()
        if (threadInfo == null || threadInfo.imageUrls.isEmpty()) {
            builder.notifyMessage("画像読込失敗", "レスからURLの抽出に失敗")
            return
        }

        // ここからカスタムview
        val view = RemoteViews(context.packageName, R.layout.notification_image)
        builder
            .setRemoteViews(view)
            .addThreadAction(POSITION_KEEP)
            .addCatalogAction()

        // 前後ボタン
        val index = intent.getIntExtra(KEY_EXTRA_IMAGE_INDEX, 0).coerceIn(0, threadInfo.imageUrls.maxIndex)
        val hasNext = index < threadInfo.imageUrls.maxIndex
        view.setOnClickOrInvisible(R.id.prev, index != 0) { builder.createViewImageIntent(index.prev) }
        view.setOnClickOrInvisible(R.id.next, hasNext) { builder.createViewImageIntent(index.next) }
        if (1 < threadInfo.imageUrls.size) {
            builder.addAction(
                NotificationCompat.Action.Builder(
                    android.R.drawable.ic_menu_more,
                    if (hasNext) "末尾" else "先頭",
                    builder.createViewImageIntent(if (hasNext) threadInfo.imageUrls.maxIndex else 0)
                ).build()
            )
        }

        // 共有ボタン
        val url = threadInfo.imageUrls[index]
        view.setOnClickPendingIntent(R.id.share, builder.createShareUrlIntent(url))
        view.setOnClickPendingIntent(R.id.image_button, builder.createShareUrlIntent(url)) // 画像タップでも共有

        // ファイル名とか
        view.setTextViewText(R.id.filename, url.pick("([^/]+$)"))
        view.setTextViewText(R.id.index, "${index.next}/${threadInfo.imageUrls.size}")
        view.setTextViewText(R.id.message, "")

        // ダウンロード前にプログレスバーを表示する
        builder
            .setProgress()
            .notifyThis()

        // 画像をダウンロード
        val (bitmap, message) = loadImage(toThumbnailUrl(url))
        view.setImageViewAny(R.id.image, bitmap)
        view.setTextViewText(R.id.message, message)

        // 表示するよ！
        builder
            .removeProgress()
            .notifyThis()
    }

}