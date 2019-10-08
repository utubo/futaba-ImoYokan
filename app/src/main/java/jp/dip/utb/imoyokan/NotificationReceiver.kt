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

        // 更新
        if (requestCode == REQUEST_CODE_RELOAD) {
            ThreadNotification().showThread(context, intent)
            return
        }

        // 返信
        val remoteInput = RemoteInput.getResultsFromIntent(intent)
        var text = remoteInput.getString(KEY_TEXT_REPLY) ?: ""
        var mail = intent.getStringExtra(KEY_EXTRA_MAIL) ?: ""
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

        if (oldMail.isNotEmpty() && mail.isEmpty() && text.isEmpty()) {
            ThreadNotification().showThread(context, intent, "返信キャンセル", "${STR_MAILADDRESS}をクリアしました")
            return
        }
        if (text.isEmpty()) {
            ThreadNotification().showThread(context, intent, "返信失敗", "本文が無いよ")
            return
        }
        GlobalScope.launch {
            val url = intent.getStringExtra(KEY_EXTRA_URL) ?: ""
            val ptua = intent.getStringExtra(KEY_EXTRA_PTUA) ?: ""
            val (title, msg) = Replyer().reply(url, text, mail, ptua)
            //val title = "テスト"; val msg = "mail=${mail},text=${text}"
            ThreadNotification().showThread(context, intent, title, msg)
        }
    }
}