package jp.dip.utb.imoyokan

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.RemoteInput
import jp.dip.utb.imoyokan.futaba.Replier
import jp.dip.utb.imoyokan.futaba.analyseCatalogUrl
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch


class NotificationReceiver : BroadcastReceiver() {

    @ExperimentalUnsignedTypes
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.getIntExtra(KEY_EXTRA_ACTION, INTENT_ACTION_RELOAD_URL)

        // URL表示
        if (action == INTENT_ACTION_RELOAD_URL) {
            val url = intent.str(KEY_EXTRA_URL)
            when {
                analyseCatalogUrl(url) != null -> CatalogNotification(context, intent).notifyThis()
                else -> ThreadNotification(context, intent).notify()
            }
            return
        }

        // 画像
        if (action == INTENT_ACTION_VIEW_IMAGE) {
            ImageNotification(context, intent).notifyThis()
            return
        }

        // 返信
        val remoteInput = RemoteInput.getResultsFromIntent(intent)
        var text = remoteInput.getString(KEY_EXTRA_REPLY_TEXT) ?: ""
        var mail = intent.str(KEY_EXTRA_MAIL)
        val oldMail = mail
        val m = "^@(\\S*)\\s+(.+)".toRegex().find(text)
        if (m != null) {
            mail = m.groupValues[1]
            text = m.groupValues[2].trim()
        } else if (text == "@") {
            mail = ""
            text = ""
        }
        intent.putExtra(KEY_EXTRA_MAIL, mail)
        val threadNotification = ThreadNotification(context, intent)

        if (oldMail.isNotEmpty() && mail.isEmpty() && text.isEmpty()) {
            threadNotification.notify("返信キャンセル", "${STR_MAIL_LABEL}をクリアしました")
            return
        }
        if (text.isEmpty()) {
            threadNotification.notify("返信失敗", "本文が無いよ")
            return
        }
        GlobalScope.launch {
            val url = intent.str(KEY_EXTRA_URL)
            val ptua = intent.str(KEY_EXTRA_PTUA)
            val (title, msg) = Replier().reply(url, text, mail, ptua)
            //val title = "テスト"; val msg = "mail=${mail},text=${text}"
            threadNotification.notify(title, msg)
        }
    }
}