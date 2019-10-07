@file:Suppress("EXPERIMENTAL_UNSIGNED_LITERALS")

package jp.dip.utb.imoyokan

import java.nio.charset.Charset

// 通知とextraData
const val NOTIFY_NAME = "ImoYokan"
const val NOTIFY_DESCRIPTION = "ImoYokan"
const val CHANNEL_ID = "imoyokan_channel"
const val KEY_EXTRA_REQUEST_CODE = "key_extra_requestCode"
const val KEY_TEXT_REPLY = "key_text_reply"
const val KEY_EXTRA_URL = "key_extra_url"
const val KEY_EXTRA_MAIL = "key_extra_mail"
const val KEY_EXTRA_PTUA = "key_extra_ptua"
const val REQUEST_CODE_RELOAD = 1
const val REQUEST_CODE_SHARE = 2
const val REQUEST_CODE_REPLY = 10
const val MAX_RES_COUNT = 10

// ふたばの設定
const val RES_INTERVAL = 36100000u
const val BOUNDARY= "BOUNDARY_ImoYoukan_BOUNDARY"
const val URL_CACHEMT = "bin/cachemt7.php"
const val USER_AGENT = "${BuildConfig.APPLICATION_ID}/${BuildConfig.VERSION_NAME}"
val FUTABA_CHARSET = charset("windows-31j")
//val jsonUrl = base + "/futaba.php?mode=json&res=" + res + "&start=" + start + "&" + Math.random()

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
        .replace("&heart;", STR_HEART)
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

fun String.around(head: String, tail: String): String {
    return head + this + tail
}

fun String.toHttps(): String {
    return this.replace("http://", "https://")
}
