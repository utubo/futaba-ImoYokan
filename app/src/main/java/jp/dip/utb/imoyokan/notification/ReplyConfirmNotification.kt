package jp.dip.utb.imoyokan.notification

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import jp.dip.utb.imoyokan.*
import jp.dip.utb.imoyokan.util.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.util.*


class ReplyConfirmNotification(private val context: Context, private val intent: Intent) {

    // 間違ったスレに書き込まないようにするため、キャッシュやintentは信じないパラメーターは全部引数で貰う
    fun notifyThis(url: String, ptua: String, mail: String, text: String, textForView: String) {
        GlobalScope.launch {
            notifyAsync(url, ptua, mail, text, textForView)
        }
    }

    private fun notifyAsync(url: String, ptua: String, mail: String, text: String, textForView: String) {

        val builder =
            ImoyokanNotificationBuilder(
                context,
                intent
            )
        val view = RemoteViews(context.packageName,
            R.layout.notification_reply_confirm
        )

        view.setTextViewText(R.id.mail, mail.ifBlank { "なし" } )
        view.setTextViewText(R.id.text, textForView.ifBlank { "本文がないよ" })

        // アクションボタンを登録
        val replyPendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_REPLY_MIN + Random().nextInt(10000), // 返信のrequestCodeはかぶらないようにする！,
            builder.createImoyokanIntent()
                .putExtra(
                    KEY_EXTRA_ACTION,
                    INTENT_ACTION_REPLY
                )
                .putExtra(KEY_EXTRA_URL, url)
                .putExtra(KEY_EXTRA_MAIL, mail)
                .putExtra(KEY_EXTRA_PTUA, ptua)
                .putExtra(KEY_EXTRA_REPLY_TEXT, text)
                .putExtra(KEY_EXTRA_CONFIRMED, true)
            ,
            PendingIntent.FLAG_UPDATE_CURRENT
        )
        builder
            .addNextPageAction(android.R.drawable.ic_menu_close_clear_cancel, "キャンセル", url, KEY_EXTRA_POSITION to POSITION_KEEP)
            .addAction(NotificationCompat.Action.Builder(android.R.drawable.ic_menu_send, "返信する", replyPendingIntent).build())

        // 表示するよ！
        builder
            .setRemoteViews(view)
            .notifyThis()
    }

}
