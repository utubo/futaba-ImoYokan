package jp.dip.utb.imoyokan

import android.content.Context
import android.content.Intent
import android.text.format.DateFormat
import android.view.View
import android.widget.RemoteViews
import jp.dip.utb.imoyokan.futaba.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.util.*


class CatalogNotification(private val context: Context, private val intent: Intent) {

    fun notifyThis() {
        GlobalScope.launch {
            notifyAsync(intent.str(KEY_EXTRA_URL))
        }
    }

    private fun notifyAsync(url: String) {

        val builder = ImoyokanNotificationBuilder(context, intent)

        val pref = Pref.getInstance(context)
        val cols = pref.catalog.cols
        val rows = pref.catalog.rows
        val catalogInfoBuilder = CatalogInfoBuilder(url, cols, rows)
        val catalogInfo = catalogInfoBuilder.buildWithoutReload()

        // URLさえ取れない場合
        if (catalogInfo.isFailed) {
            builder.notifyMessage("カタログ取得失敗", catalogInfo.message)
            return
        }

        // カタログの並び順とかを保存する
        pref.catalog.sort = catalogInfo.sort
        pref.lastCatalogUrl = url
        pref.apply()

        // スレッドのスクロール位置とかの情報はクリア
        intent.removeExtra(KEY_EXTRA_POSITION)
        intent.removeExtra(KEY_EXTRA_IMAGE_INDEX)

        // URLが取れたならボタンも作れる
        builder
            .addCatalogAction("カタログ", catalogInfo, SORT_DEFAULT)
            .addCatalogAction("新順", catalogInfo, SORT_NEWER)
            .addCatalogAction("多順", catalogInfo, SORT_REPLY)

        // まずはプログレスバーを表示する
        builder
            .setProgress()
            .notifyThis()

        // HTML読み込みと解析
        catalogInfoBuilder.reload()
        if (catalogInfo.isFailed) {
            builder.notifyMessage("カタログ取得失敗", catalogInfo.message)
            return
        }

        // 画像読み込み
        val view = RemoteViews(context.packageName, R.layout.notification_catalog)
        var index = 0
        var x = 1
        var y = 1
        run loop@ { catalogInfo.items.forEach {
            index++
            // URLをセット
            val id = context.resources.getIdentifier("cat_${x}_${y}", "id", context.packageName)
            view.setOnClickPendingIntent(id, builder.createNextPageIntent(it.href))
            // 画像をセット
            val (bitmap, _) = loadImage(it.img)
            view.setImageViewAny(id, bitmap ?: android.R.drawable.ic_delete)
            view.setViewVisibility(id, View.VISIBLE)

            // プログレスバー更新はやらない
            // (notifyしすぎるとAndroidが完了時のnotifyを捨てちゃうのでやめた)
            //notificationBuilder.setProgress(COLS * ROWS, index, false)
            //notificationManager.notify(0, notificationBuilder.build())

            // 次の位置へ
            x ++
            if (cols < x) {
                x = 1
                y ++
                if (rows < y) {
                    return@loop
                }
            }
        }}

        // 表示するよ！
        builder
            .removeProgress()
            .setRemoteViews(view)
            .notifyThis()
    }

    private fun ImoyokanNotificationBuilder.addCatalogAction(text: String, catalogInfo: CatalogInfo, sort: String): ImoyokanNotificationBuilder {
        val sb = StringBuilder(text)
        if (catalogInfo.sort == sort) {
            sb.append(DateFormat.format("(HH:mm:ss)", Date()))
        }
        this.addNextPageAction(android.R.drawable.ic_menu_sort_by_size, sb.toString(), getCatalogUrl(catalogInfo.url, sort))
        return this
    }
}