package jp.dip.utb.imoyokan.futaba

import android.graphics.Color
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import jp.dip.utb.imoyokan.BuildConfig
import jp.dip.utb.imoyokan.aroundWhenIsNotEmpty
import jp.dip.utb.imoyokan.pick
import jp.dip.utb.imoyokan.toHttps

@Suppress("EXPERIMENTAL_UNSIGNED_LITERALS")
const val RES_INTERVAL = 36100000u
const val BOUNDARY= "BOUNDARY_ImoYokan_BOUNDARY"
const val URL_CACHEMT = "bin/cachemt7.php"
const val USER_AGENT = "${BuildConfig.APPLICATION_ID}/${BuildConfig.VERSION_NAME}"
val FUTABA_CHARSET = charset("windows-31j")
const val QUOTE_COLOR = "#DE789922" // TODO: "DE"はAndroidのprimary textの不透明度)
const val SORT_DEFAULT = ""
const val SORT_NEWER = "1"
const val SORT_REPLY = "3"
val KITAA_REGEX = "ｷﾀ━━+\\(ﾟ∀ﾟ\\)━━+".toRegex()
const val SHORT_KITAA = "ｷﾀ━(ﾟ∀ﾟ)━"
const val SIO_KARA_SU_ROOT = "http://www.nijibox5.com/futabafiles/tubu/src/"

fun String.toColoredText(br:String = "\n"): SpannableStringBuilder {
    val sb = SpannableStringBuilder()
    val quoteStyle = ForegroundColorSpan(Color.parseColor(QUOTE_COLOR))
    this.split(br).forEach { line ->
        if (sb.isNotEmpty()) {
            sb.append(br)
        }
        if (line.startsWith(">")) {
            val start = sb.length
            sb.append(line)
            val end = sb.length
            sb.setSpan(quoteStyle, start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        } else {
            sb.append(line)
        }
    }
    return sb
}

fun analyseUrl(url: String): Triple<String, String, String>? {
    val urlMatches = "(https?://.*\\.2chan\\.net)/([^/]+)/res/(\\d+).htm".toRegex().find(url)?.groupValues ?: return null
    val server = urlMatches[1]
    val b: String = urlMatches[2]
    val res = urlMatches[3]
    return Triple(server, b, res)
}

fun analyseCatalogUrl(url: String): Triple<String, String, String>? {
    val urlMatches = "(https?://.*\\.2chan\\.net)/([^/]+)/futaba.php\\?mode=cat(.*)".toRegex().find(url)?.groupValues ?: return null
    val server = urlMatches[1]
    val b: String = urlMatches[2]
    val sort: String = urlMatches[3].pick("&sort=([^&]+)".toRegex())
    return Triple(server, b, sort)
}

fun getCatalogUrl(url: String, sort:String = ""): String {
    val root = url.pick("(^https?://.*\\.2chan\\.net/[^/]+)".toRegex())
    return "$root/futaba.php?mode=cat${aroundWhenIsNotEmpty("&sort=", sort, "")}"
}

fun toThumbnailUrl(url: String): String {
    return if (url.startsWith(SIO_KARA_SU_ROOT)) {
            url.replace("src", "misc").replace(".png|.jpg|.jpeg|.webm|.mp4".toRegex(), ".thumb.jpg")
        } else {
            url.toHttps().replace("/src/", "/thumb/").replace(".png|.jpg|.jpeg|.webm|.mp4".toRegex(), "s.jpg")
        }
}

fun toCatalogImageUrl(url: String): String {
    return if (url.startsWith(SIO_KARA_SU_ROOT)) {
        toThumbnailUrl(url)
    } else {
        toThumbnailUrl(url).replace("/thumb/", "/cat/")
    }
}

