package jp.dip.utb.imoyokan.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.RemoteInput
import jp.dip.utb.imoyokan.futaba.presenter.Replier
import jp.dip.utb.imoyokan.futaba.util.analyseCatalogUrl
import jp.dip.utb.imoyokan.model.Pref
import jp.dip.utb.imoyokan.notification.*
import jp.dip.utb.imoyokan.util.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch


class NotificationReceiver : BroadcastReceiver() {

    @ExperimentalUnsignedTypes
    override fun onReceive(context: Context, intent: Intent) {
        val pref = Pref.getInstance(context)
        val url = intent.str(KEY_EXTRA_URL)
        if (url.isNotBlank()) {
            pref.lastUrl = url
            pref.apply()
        }

        val action = intent.getIntExtra(
            KEY_EXTRA_ACTION,
            INTENT_ACTION_RELOAD_URL
        )

        // URL表示
        if (action == INTENT_ACTION_RELOAD_URL) {
            when {
                analyseCatalogUrl(url) != null -> CatalogNotification(
                    context,
                    intent
                ).notifyThis()
                else -> ThreadNotification(
                    context,
                    intent
                ).notify()
            }
            return
        }

        // 画像
        if (action == INTENT_ACTION_VIEW_IMAGE) {
            ImageNotification(
                context,
                intent
            ).notifyThis()
            return
        }

        // メアド設定画面表示
        if (action == INTENT_ACTION_GO_SET_MAIL) {
            MailSettingNotification(
                context,
                intent
            ).notifyThis()
            return
        }

        // メアド設定
        if (action == INTENT_ACTION_SET_MAIL) {
            var mail = if (intent.hasExtra(KEY_EXTRA_MAIL)) intent.str(
                KEY_EXTRA_MAIL
            ) else RemoteInput.getResultsFromIntent(intent).getString(KEY_EXTRA_MAIL, "")
            mail = mail.replace("^[@＠]".toRegex(), "") // 返信欄での直接設定と統一するため先頭＠は削除しておく
            pref.mail.set(mail, pref.lastThreadUrl)
            // スレッドに戻ったほうがタップ回数少ないけど解りづらいかな…
            //when {
            //    mail.isBlank() -> {
            //        ThreadNotification(context, intent).notifyCache("メールアドレスをクリアしました")
            //    }
            //    else -> {
            //        ThreadNotification(context, intent).notifyCache("メールアドレスをセットしました", mail)
            //    }
            //}
            intent.putExtra(KEY_EXTRA_MAIL, mail)
            MailSettingNotification(
                context,
                intent
            ).notifyThis()
            return
        }

        // 返信
        val isConfirmed = intent.hasExtra(KEY_EXTRA_CONFIRMED)
        val inputText = when {
            isConfirmed -> intent.getStringExtra(KEY_EXTRA_REPLY_TEXT) ?: ""
            else -> RemoteInput.getResultsFromIntent(intent).getString(KEY_EXTRA_REPLY_TEXT, "")
        }

        // メールアドレスを設定する
        val defaultMail = intent.str(KEY_EXTRA_MAIL) // 基本的にintentにある(通知に表示中)のメアドが正義(prefのメアドは使わない)
        var (mail, text) = pickMailAndText(defaultMail, inputText, pref) // ただし入力で上書きされることがある
        if (mail != defaultMail || text.isNotBlank()) {
            pref.mail.set(mail, url)
            pref.apply()
        }
        if (text.isBlank()) {
            // 本文がないときはメールアドレス設定の結果を表示するスペースがある
            intent.putExtra(
                KEY_EXTRA_POSITION,
                POSITION_KEEP
            )
            intent.putExtra(KEY_EXTRA_USE_CACHE, true)
            when {
                defaultMail == mail -> {
                    ThreadNotification(
                        context,
                        intent
                    ).notifyCache("本文が無いよ")
                }
                mail.isBlank() -> {
                    ThreadNotification(
                        context,
                        intent
                    ).notifyCache("メールアドレスをクリアしました")
                }
                else -> {
                    ThreadNotification(
                        context,
                        intent
                    )
                        .notifyCache("メールアドレスをセットしました", mail)
                }
            }
            return
        }

        // 本文があるなら返信するよ
        text = text.addLineBreakForSingleLineInput()
        intent.putExtra(
            KEY_EXTRA_POSITION,
            THREAD_BOTTOM
        )
        when {
            pref.confirmBeforeReply && !isConfirmed -> {
                ReplyConfirmNotification(
                    context,
                    intent
                ).notifyThis(url, intent.str(
                    KEY_EXTRA_PTUA
                ), mail, inputText, text)
            }
            pref.debugMode -> {
                ThreadNotification(
                    context,
                    intent
                )
                    .notify("Debug - 返信キャンセル", "mail=${mail},text=${text}")
            }
            else -> {
                GlobalScope.launch {
                    val (title, msg) = Replier()
                        .reply(url, text, mail, intent.str(
                        KEY_EXTRA_PTUA
                    ))
                    ThreadNotification(
                        context,
                        intent
                    )
                        .notify(title, msg)
                }
            }
        }
    }


    private fun pickMailAndText(defaultMail: String, text: String, pref: Pref): Pair<String, String> {
        val regex = when {
            pref.mail.ignoreWideChar -> """^[@＠]([^ 　]*)[ 　]+(.*)""".toRegex()
            else -> """^@([^ ]*) +(.*)""".toRegex()
        }
        val m = regex.find(text)
        return when {
            m != null -> {
                m.groupValues[1] to m.groupValues[2].trim()
            }
            text == "@" || pref.mail.ignoreWideChar && text == "＠" -> {
                (if (defaultMail.isBlank()) pref.mail.lastNoBlank else "") to ""
            }
            else -> {
                defaultMail to text
            }
        }
    }

}