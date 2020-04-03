package jp.dip.utb.imoyokan

import android.content.Context
import android.content.Intent
import android.os.Build
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.util.Log
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
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
        val threadInfo = Cache(context).loadThreadInfo()
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
        val filenameSpan = SpannableStringBuilder(url.pick("([^/]+$)"))
        val ext = url.pick("(\\.\\w+)$")
        if (".gif.webm.mp4".contains(ext)) {
            filenameSpan.setSpan(
                ForegroundColorSpan(ContextCompat.getColor(context, R.color.textColorAccent)),
                filenameSpan.indexOf(ext),
                filenameSpan.length,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }
        view.setTextViewText(R.id.filename, filenameSpan)
        view.setTextViewText(R.id.index, "${index.next}/${threadInfo.imageUrls.size}")
        view.setTextViewText(R.id.message, "")

        // ダウンロード前にプログレスバーを表示する
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            // Android9以降はバグで表示領域が広がりきってるとプログレスバーを表示できない
            view.setTextViewText(R.id.message, "⏳")
        } else {
            builder.setProgress()
        }
        builder.notifyThis()

        // ダウンロードには時間がかかるので読み込み後別の画面かもしれない
        val beforePostTime = builder.getLastNotification()?.postTime

        // 画像をダウンロード
        val (bitmap, message) = loadImage(toThumbnailUrl(url))
        view.setImageViewAny(R.id.image, bitmap)
        view.setTextViewText(R.id.message, message)

        val afterPostTime = builder.getLastNotification()?.postTime
        if (beforePostTime != null && afterPostTime != null && beforePostTime != afterPostTime) {
            Log.d(NOTIFY_NAME, "画面切り替えずみなので表示をキャンセルしました")
            return
        }

        // 表示するよ！
        builder
            .removeProgress()
            .notifyThis()
    }

}