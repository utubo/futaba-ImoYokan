package jp.dip.utb.imoyokan.futaba.util

import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import jp.dip.utb.imoyokan.*
import jp.dip.utb.imoyokan.util.aroundOrEmpty
import jp.dip.utb.imoyokan.util.pick
import jp.dip.utb.imoyokan.util.toHttps

@Suppress("EXPERIMENTAL_UNSIGNED_LITERALS")
const val RES_INTERVAL = 36100000u
const val URL_CACHEMT = "bin/cachemt7.php"
const val USER_AGENT = "${BuildConfig.APPLICATION_ID}/${BuildConfig.VERSION_NAME}"
val FUTABA_CHARSET = charset("windows-31j")
const val SORT_DEFAULT = ""
const val SORT_NEWER = "1"
const val SORT_REPLY = "3"
const val CATALOG_TEXT_LENGTH = 20

val KITAA_REGEX = """ｷﾀ━━+\(ﾟ∀ﾟ\)━━+""".toRegex()
const val SHORT_KITAA = "ｷﾀ━(ﾟ∀ﾟ)━"

const val IMAGE_EXT = """\.(jpg|jpeg|png|gif|webm|mp4|webp)"""
val IMAGE_EXT_REGEX = IMAGE_EXT.toRegex()

fun String.toColoredText(quoteColor: Int, br:String = "\n"): Spannable {
    val sb = SpannableString(this)
    var start = 0
    while (start < sb.length) {
        val end = sb.indexOf(br, start + 1).let { if (it == -1) sb.length else it }
        if (sb[start] == '>') {
            sb.setSpan(ForegroundColorSpan(quoteColor), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        start = end + 1
    }
    return sb
}

fun analyseUrl(url: String): Triple<String, String, String>? {
    val urlMatches = """(https?://.*\.2chan\.net)/([^/]+)/res/(\d+).htm""".toRegex().find(url)?.groupValues ?: return null
    val server = urlMatches[1]
    val b: String = urlMatches[2]
    val res = urlMatches[3]
    return Triple(server, b, res)
}

fun analyseCatalogUrl(url: String): Triple<String, String, String>? {
    val urlMatches = """(https?://.*\.2chan\.net)/([^/]+)/futaba.php\?mode=cat(.*)""".toRegex().find(url)?.groupValues ?: return null
    val server = urlMatches[1]
    val b: String = urlMatches[2]
    val sort: String = urlMatches[3].pick("&sort=([^&]+)".toRegex())
    return Triple(server, b, sort)
}

fun getCatalogUrl(url: String, sort:String = ""): String {
    val root = url.pick("""(^https?://.*\.2chan\.net/[^/]+)""".toRegex())
    return "$root/futaba.php?mode=cat${aroundOrEmpty(
        "&sort=",
        sort,
        ""
    )}"
}

fun toThumbnailUrl(url: String): String {
    return if (SIO_FILE_REGEX.containsMatchIn(url)) {
        getSiokaraThumbnailUrl(url)
        } else {
            url.toHttps().replace("/src/", "/thumb/").replace(IMAGE_EXT_REGEX, "s.jpg")
        }
}

fun toCatalogImageUrl(url: String): String {
    return toThumbnailUrl(url)
        .replace("/thumb/", "/cat/")
}
