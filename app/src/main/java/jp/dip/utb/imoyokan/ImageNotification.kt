package jp.dip.utb.imoyokan

import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import com.squareup.picasso.Picasso
import jp.dip.utb.imoyokan.futaba.SIO_KARA_REGEX
import jp.dip.utb.imoyokan.futaba.SIO_KARA_SU_ROOT
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
        val view = RemoteViews(context.packageName, R.layout.notification_image).also { it.setViewVisibility(R.id.message, View.GONE) }
        builder
            .setRemoteViews(view)
            .addThreadAction(POSITION_KEEP)
            .addCatalogAction()

        // 前後ボタン
        val index = intent.getIntExtra(KEY_EXTRA_IMAGE_INDEX, 0).coerceIn(0, threadInfo.imageUrls.maxIndex)
        val hasNext = index < threadInfo.imageUrls.maxIndex
        view.setOnClickOrGone(R.id.prev, index != 0) { builder.createViewImageIntent(index.prev) }
        view.setOnClickOrGone(R.id.next, hasNext) { builder.createViewImageIntent(index.next) }
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

        // ファイル名とか
        view.setTextViewText(R.id.filename, url.pick("([^/]+$)"))
        view.setTextViewText(R.id.index, "${index.next}/${threadInfo.imageUrls.size}")

        // ダウンロード前にプログレスバーを表示する
        builder
            .setProgress()
            .notifyThis()

        // 画像をダウンロード
        try {
            val bitmap = Picasso.get().load(toThumbnailUrl(url)).get()
            view.setImageViewBitmap(R.id.image, bitmap)
        } catch (e: Exception) {
            view.setImageViewBitmap(R.id.image, null)
            view.setTextViewText(R.id.message, e.message)
            view.setViewVisibility(R.id.message, View.VISIBLE)
        }

        // 表示するよ！
        builder
            .removeProgress()
            .notifyThis()
    }

}