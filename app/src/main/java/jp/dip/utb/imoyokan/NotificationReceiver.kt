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
        val url = intent.str(KEY_EXTRA_URL)

        // URL表示
        if (action == INTENT_ACTION_RELOAD_URL) {
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

        val pref = Pref.getInstance(context)

        // メアドクリア
        if (action == INTENT_ACTION_CLEAR_MAIL) {
            pref.mail.set("", pref.lastThreadUrl)
            ThreadNotification(context, intent).notify("メールアドレスをクリアしました")
            return
        }

        // 返信
        val remoteInput = RemoteInput.getResultsFromIntent(intent)

        // メールアドレスを設定する
        val defaultMail = intent.str(KEY_EXTRA_MAIL) // 書き込むときは基本的にintentにある(通知に表示中)のメアドが正義
        val (mail, text) = pickMailAndText(defaultMail, remoteInput.getString(KEY_EXTRA_REPLY_TEXT, ""), pref) // ただし入力で上書きされることがある
        if (mail != defaultMail || text.isNotBlank()) {
            pref.mail.set(mail, url)
            pref.apply()
        }
        val threadNotification = ThreadNotification(context, intent)
        if (text.isEmpty()) {
            // 本文がないときはメールアドレス設定の結果を表示するスペースがある
            if (defaultMail == mail) {
                threadNotification.notify("本文が無いよ")
            } else if (mail.isBlank()) {
                threadNotification.notify("メールアドレスをクリアしました")
            } else {
                threadNotification.notify("メールアドレスをセットしました", mail)
            }
            return
        }

        // 本文があるなら返信するよ
        GlobalScope.launch {
            if (pref.debugMode) {
                threadNotification.notify("Debug - 返信キャンセル", "mail=${mail},text=${text}")
            } else {
                val (title, msg) = Replier().reply(url, text, mail, intent.str(KEY_EXTRA_PTUA))
                threadNotification.notify(title, msg)
            }
        }
    }

    private fun pickMailAndText(defaultMail: String, text: String, pref: Pref): Pair<String, String> {
        val regex = if (pref.mail.ignoreWideChar) {
            """^[@＠]([^ 　]*)[ 　]+(.*)""".toRegex()
        } else {
            """^@([^ ]*) +(.*)""".toRegex()
        }
        val m = regex.find(text)
        return if (m != null) {
            m.groupValues[1] to m.groupValues[2].trim()
        } else if (text == "@" || pref.mail.ignoreWideChar && text == "＠") {
            (if (defaultMail.isBlank()) pref.mail.lastNoBlank else "") to ""
        } else {
            defaultMail to text
        }
    }

}