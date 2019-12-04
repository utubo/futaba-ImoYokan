@file:Suppress("EXPERIMENTAL_UNSIGNED_LITERALS")

package jp.dip.utb.imoyokan

import android.content.Intent
import android.util.Log
import android.view.View
import com.squareup.picasso.Picasso
import java.nio.charset.Charset

// 通知とextraData
const val NOTIFY_NAME = "ImoYokan"
const val NOTIFY_DESCRIPTION = "ImoYokan"
const val CHANNEL_ID = "imoyokan_channel"
const val KEY_EXTRA_ACTION = "key_extra_action"
const val KEY_EXTRA_IMAGE_INDEX = "key_extra_image_index"
const val KEY_EXTRA_MAIL = "key_extra_mail"
const val KEY_EXTRA_POSITION = "key_extra_position"
const val KEY_EXTRA_PTUA = "key_extra_ptua"
const val KEY_EXTRA_REPLY_TEXT = "key_extra_reply_text"
const val KEY_EXTRA_URL = "key_extra_url"
const val INTENT_ACTION_VIEW_IMAGE = 99
const val INTENT_ACTION_REPLY = 100
const val INTENT_ACTION_RELOAD_URL = 15000
const val REQUEST_CODE_REPLY_MIN = 100
const val REQUEST_CODE_RELOAD_URL_MIN = 15000
const val POSITION_KEEP = 10000
const val THREAD_BOTTOM = 20000

// 表示設定
const val MAX_RES_COUNT = 10

// 記号
const val STR_MAIL_LABEL = "📧"
const val STR_HEARTS = "❤"

// ユーティリティ
fun String.removeHtmlTag(): String {
    var s = this
        .replace("<br>", "\n", true)
        .replace("<[^>]+>".toRegex(), "&#0;")
        .replace("&gt;", ">", true)
        .replace("&lt;", "<", true)
        .replace("&quot;", "\"", true)
        .replace("&hearts;", STR_HEARTS)
        .replace("&zwj;", "&#8205;")
    s = "&#(\\d{2,});".toRegex().replace(s) {
        val code = it.groupValues[1].toInt()
        if (code < 32)
            ""
        else
            String(Character.toChars(code))
    }
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

fun String.addLineBreakForSingleLineInput(replaceTarget: String = "  "): String {
    return this.replace(replaceTarget, "\n")
}

fun aroundWhenIsNotEmpty(head: String, text: String?, tail: String): String {
    return if (text?.isNotBlank() == true) (head + text + tail) else ""
}

fun String.toHttps(): String {
    return this.replace("http://", "https://")
}

fun String.pick(regex: Regex, g: Int = 1): String {
    return regex.find(this)?.groupValues?.get(g) ?: ""
}

fun String.pick(regex: String, g: Int = 1): String {
    return this.pick(regex.toRegex(), g)
}

/** ?.groupValues?.get(index)?.let と同じ */
fun MatchResult.forGroupValue(index: Int, f: (String) -> Unit) {
    f(this.groupValues[index])
}

val Int.prev: Int get() { return this - 1 }
val Int.next: Int get() { return this + 1 }

val List<*>.maxIndex: Int get() {
    return this.size - 1
}

fun Intent.str(key: String): String {
    return this.getStringExtra(key) ?: ""
}

fun Intent.putAll(vararg extras: Pair<String, Any>): Intent {
    extras.forEach {
        when (it.second) {
            is Int -> this.putExtra(it.first, it.second as Int)
            is String -> this.putExtra(it.first, it.second as String)
            is Boolean -> this.putExtra(it.first, it.second as Boolean)
        }
    }
    return this
}

fun visibleOrGone(b: Boolean): Int {
    return if (b) View.VISIBLE else View.GONE
}

fun loadImage(url: String?): Pair<Any?, String> {
    return try {
        when {
            url == null -> Pair(null, "Empty URL")
            url.isBlank() -> Pair(null, "Empty URL")
            url.endsWith(".mp4") -> Pair(R.drawable.ic_video, "")
            url.endsWith(".webm") -> Pair(R.drawable.ic_video, "")
            else -> Pair(Picasso.get().load(url).get(), "")
        }
    } catch (e: Throwable) {
        Log.d(NOTIFY_NAME, "画像読み込み失敗 url=${url}", e)
        Pair(null, e.message ?: "")
    }
}
