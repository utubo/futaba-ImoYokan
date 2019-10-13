package jp.dip.utb.imoyokan

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.RemoteInput
import jp.dip.utb.imoyokan.futaba.Replyer
import jp.dip.utb.imoyokan.futaba.analyseCatalogUrl
import jp.dip.utb.imoyokan.futaba.analyseImageUrl
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch


class NotificationReceiver : BroadcastReceiver() {

    @ExperimentalUnsignedTypes
    override fun onReceive(context: Context, intent: Intent) {
        val requestCode = intent.getIntExtra(KEY_EXTRA_REQUEST_CODE, 0)

        // URL表示
        if (requestCode < REQUEST_CODE_REPLY) {
            val url = intent.str(KEY_EXTRA_URL)
            when {
                analyseCatalogUrl(url) != null -> CatalogNotification(context, intent).notifyThis()
                analyseImageUrl(url) != null -> ImageNotification(context, intent).notifyThis()
                else -> ThreadNotification(context, intent).notify()
            }
            return
        }

        // 返信
        val remoteInput = RemoteInput.getResultsFromIntent(intent)
        var text = remoteInput.getString(KEY_TEXT_REPLY) ?: ""
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
            threadNotification.notify("返信キャンセル", "${STR_MAILADDRESS}をクリアしました")
            return
        }
        if (text.isEmpty()) {
            threadNotification.notify("返信失敗", "本文が無いよ")
            return
        }
        GlobalScope.launch {
            val url = intent.str(KEY_EXTRA_URL)
            val ptua = intent.str(KEY_EXTRA_PTUA)
            val (title, msg) = Replyer().reply(url, text, mail, ptua)
            //val title = "テスト"; val msg = "mail=${mail},text=${text}"
            threadNotification.notify(title, msg)
        }
    }
}