@file:Suppress("EXPERIMENTAL_UNSIGNED_LITERALS")

package jp.dip.utb.imoyokan

import android.content.Intent
import java.nio.charset.Charset

// 通知とextraData
const val NOTIFY_NAME = "ImoYokan"
const val NOTIFY_DESCRIPTION = "ImoYokan"
const val CHANNEL_ID = "imoyokan_channel"
const val KEY_EXTRA_REQUEST_CODE = "key_extra_requestCode"
const val KEY_TEXT_REPLY = "key_text_reply"
const val KEY_EXTRA_MAIL = "key_extra_mail"
const val KEY_EXTRA_PTUA = "key_extra_ptua"
const val KEY_EXTRA_SORT = "key_extra_sort"
const val REQUEST_CODE_SHARE = 99
const val REQUEST_CODE_RELOAD = 100
const val REQUEST_CODE_REPLY = 300

// 表示設定
const val MAX_RES_COUNT = 10

// 記号
const val STR_MAILADDRESS = "✉"
const val STR_HEART = "❤"

// ユーティリティ
fun String.removeHtmlTag(): String {
    var s = this
        .replace("<br>", "\n", true)
        .replace("<[^>]+>".toRegex(), "&#0;")
        .replace("&gt;", ">", true)
        .replace("&lt;", "<", true)
        .replace("&hearts;", STR_HEART)
    s = "&#(\\d{4,});".toRegex().replace(s) { String(Character.toChars(it.groupValues[1].toInt())) }
    return s
        .replace("&#0;", "")
        .replace("&amp;", "&")
}

fun String.replaceForPost(charset: Charset): String {
    // TODO: これSHIFT-JIS専用になっちゃってるな…
    var s = this
        .replace('\u301C', '\uFF5E') // 波ダッシュ
        .replace('\u2212', '\uFF0D') // 全角マイナス
        .replace('\u00A2', '\uFFE0') // ￠
        .replace('\u00A3', '\uFFE1') // ￡
        .replace('\u00AC', '\uFFE2') // ￢
        .replace('\u2014', '\u2015') // 全角ダッシュ
        .replace('\u2016', '\u2225') // ∥
    // 絵文字とか
    val e = charset.newEncoder()
    s = ".".toRegex().replace(s) {
        if (e.canEncode(it.value)) {
            it.value
        } else {
            "&#${it.value.codePointAt(0)};"
        }
    }
    return s
}

fun aroundWhenIsNotEmpty(head: String, text: String?, tail: String): String {
    return if (text?.isNotBlank() == true) (head + text + tail) else ""
}

fun String.toHttps(): String {
    return this.replace("http://", "https://")
}

fun Intent.str(key: String): String {
    return this.getStringExtra(key) ?: ""
}
