package jp.dip.utb.imoyokan

import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch


class MailSettingNotification(private val context: Context, private val intent: Intent) {

    // 間違ったスレに書き込まないようにするため、キャッシュやintentは信じないパラメーターは全部引数で貰う
    fun notifyThis() {
        GlobalScope.launch {
            notifyAsync()
        }
    }

    private fun notifyAsync() {

        val builder = ImoyokanNotificationBuilder(context, intent)
        val view = RemoteViews(context.packageName, R.layout.notification_mail_setting)
        val mail = intent.str(KEY_EXTRA_MAIL)

        view.setTextViewText(R.id.mail, mail.ifBlank { "なし" } )

        // アクションボタンを登録
        // メール入力欄
        builder.addRemoteInput(
            R.drawable.ic_edit,
            "変更",
            "ﾒｰﾙｱﾄﾞﾚｽを入力してください",
            KEY_EXTRA_MAIL,
            builder.createImoyokanIntent().putExtra(KEY_EXTRA_ACTION, INTENT_ACTION_SET_MAIL)
        )

        // クリアボタン
        if (mail.isNotBlank()) {
            val clearAction = NotificationCompat.Action
                .Builder(android.R.drawable.ic_menu_delete, "クリア", builder.createPendingIntent(KEY_EXTRA_ACTION to INTENT_ACTION_SET_MAIL, KEY_EXTRA_MAIL to ""))
                .build()
            builder.addAction(clearAction)
        }

        // 復活ボタン
        if (mail.isBlank() && Pref.getInstance(context).mail.lastNoBlank.isNotBlank()) {
            val undoAction = NotificationCompat.Action
                .Builder(android.R.drawable.ic_menu_upload, "復活", builder.createPendingIntent(KEY_EXTRA_ACTION to INTENT_ACTION_SET_MAIL, KEY_EXTRA_MAIL to Pref.getInstance(context).mail.lastNoBlank))
                .build()
            builder.addAction(undoAction)
        }

        // スレッドボタン
        builder.addThreadAction(POSITION_KEEP)

        // 表示するよ！
        builder
            .setRemoteViews(view)
            .notifyThis()
    }

}
