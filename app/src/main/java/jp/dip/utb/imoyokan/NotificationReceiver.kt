package jp.dip.utb.imoyokan

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.RemoteInput
import jp.dip.utb.imoyokan.futaba.Replyer
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch


class NotificationReceiver : BroadcastReceiver() {

    @ExperimentalUnsignedTypes
    override fun onReceive(context: Context, intent: Intent) {
        val requestCode = intent.getIntExtra(KEY_EXTRA_REQUEST_CODE, 0)
        val url = intent.getStringExtra(KEY_EXTRA_URL) ?: ""
        var mail = intent.getStringExtra(KEY_EXTRA_MAIL) ?: ""

        // 更新
        if (requestCode == REQUEST_CODE_RELOAD) {
            ThreadNotification().showThread(context, url, mail)
            return
        }

        // 返信
        val remoteInput = RemoteInput.getResultsFromIntent(intent)
        var text = remoteInput.getString(KEY_TEXT_REPLY) ?: ""
        val oldMail = mail
        val m = "^@(\\S*)\\s+(.+)".toRegex().find(text)
        if (m != null) {
            mail = m.groupValues[1]
            text = m.groupValues[2].trim()
        } else if (text == "@") {
            mail = ""
            text = ""
        }
        if (oldMail.isNotEmpty() && mail.isEmpty() && text.isEmpty()) {
            ThreadNotification().showThread(context, url, mail,"返信キャンセル", "${STR_MAILADDRESS}をクリアしました")
            return
        }
        if (text.isEmpty()) {
            ThreadNotification().showThread(context, url, mail,"返信失敗", "本文が無いよ")
            return
        }
        GlobalScope.launch {
            val (title, msg) = Replyer().reply(intent, url, text, mail)
            //val title = "テスト"; val msg = "mail=${mail},text=${text}"
            ThreadNotification().showThread(context, url, mail, title, msg)
        }
    }


}