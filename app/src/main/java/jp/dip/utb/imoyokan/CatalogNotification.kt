package jp.dip.utb.imoyokan

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.text.format.DateFormat
import android.view.View
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.squareup.picasso.Picasso
import jp.dip.utb.imoyokan.futaba.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.util.*


class CatalogNotification(private val context: Context, private val intent: Intent) {

    companion object {
        const val COLS = 7
        const val ROWS = 4
    }

    fun notifyThis() {
        GlobalScope.launch {
            notifyAsync(intent.str(Intent.EXTRA_TEXT))
        }
    }

    private fun notifyAsync(url: String) {

        var requestCode = REQUEST_CODE_RELOAD
        val notificationBuilder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_imoyokan)

        val catalogInfoBuilder = CatalogInfoBuilder(url, COLS, ROWS)
        val catalogInfo = catalogInfoBuilder.buildWithoutReload()

        // URLさえ取れない場合
        if (catalogInfo.isFailed) {
            notificationBuilder
                .setContentTitle("カタログ取得失敗")
                .setContentText(catalogInfo.message)
            notificationBuilder.notifySilent(context, CHANNEL_ID)
            return
        }

        // URLが取れたならボタンも作れる
        notificationBuilder
            .addAction(createCatalogAction(context, ++requestCode, "カタログ", catalogInfo, SORT_DEFAULT))
            .addAction(createCatalogAction(context, ++requestCode, "新順", catalogInfo, SORT_NEWER))
            .addAction(createCatalogAction(context, ++requestCode, "多順", catalogInfo, SORT_REPLY))

        // まずはプログレスバーを表示する
        val notificationManager = NotificationManagerCompat.from(context) // こいつで表示を更新する
        notificationBuilder
            .setProgress(COLS * ROWS, 0, true /* = ずっとぐるぐるまわるスタイル */ )
            .notifySilent(context, CHANNEL_ID)

        // HTML読み込みと解析
        catalogInfoBuilder.reload()
        if (catalogInfo.isFailed) {
            notificationBuilder
                .removeProgress()
                .setContentTitle("カタログ取得失敗")
                .setContentText(catalogInfo.message)
            notificationManager.notify(0, notificationBuilder.build())
            return
        }

        // 画像読み込み
        val view = RemoteViews(context.packageName, R.layout.notification_catalog)
        var index = 0
        var x = 0
        var y = 0
        run loop@ { catalogInfo.items.forEach {
            index++
            // 画像をセット
            val id = context.resources.getIdentifier("cat_${x}_${y}", "id", context.packageName)
            val clickIntent = Intent(context, NotificationReceiver::class.java)
                .putExtra(KEY_EXTRA_REQUEST_CODE, REQUEST_CODE_RELOAD)
                .putExtra(KEY_EXTRA_SORT, catalogInfo.sort)
                .putExtra(Intent.EXTRA_TEXT, it.href)
            view.setOnClickPendingIntent(id, PendingIntent.getBroadcast(context, ++requestCode, clickIntent, PendingIntent.FLAG_CANCEL_CURRENT))
            if (it.img != null) {
                val bitmap = Picasso.get().load(it.img).get()
                view.setImageViewBitmap(id, bitmap)
            }
            view.setViewVisibility(id, View.VISIBLE)

            // プログレスバー更新はやらない
            // (notifyしすぎるとAndroidが完了時のnotifyを捨てちゃうのでやめた)
            //notificationBuilder.setProgress(COLS * ROWS, index, false)
            //notificationManager.notify(0, notificationBuilder.build())

            // 次の位置へ
            x ++
            if (COLS <= x) {
                x = 0
                y ++
                if (ROWS <= y) {
                    return@loop
                }
            }
        }}

        // 表示するよ！
        notificationBuilder
            .removeProgress()
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
            .setCustomBigContentView(view)
            .setContent(view)
        notificationManager.notify(0, notificationBuilder.build())
    }

    private fun createCatalogAction(context: Context, requestCode: Int, text: String, catalogInfo: CatalogInfo, sort: String): NotificationCompat.Action {
        val intent = Intent(context, NotificationReceiver::class.java)
        intent.putExtra(Intent.EXTRA_TEXT, getCatalogUrl(catalogInfo.url, sort))
        intent.putExtra(KEY_EXTRA_REQUEST_CODE, REQUEST_CODE_RELOAD)
        val sb = StringBuilder(text)
        if (catalogInfo.sort == sort) {
            sb.append(DateFormat.format("(HH:mm:ss)", Date()))
        }
        return NotificationCompat.Action.Builder(
            android.R.drawable.ic_menu_sort_by_size,
            sb.toString(),
            PendingIntent.getBroadcast(context, requestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT)
        ).build()
    }
}