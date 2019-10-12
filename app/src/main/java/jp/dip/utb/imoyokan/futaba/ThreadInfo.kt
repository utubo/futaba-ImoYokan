package jp.dip.utb.imoyokan.futaba

import android.graphics.Bitmap
import com.github.kittinunf.fuel.httpGet
import com.github.kittinunf.result.Result
import com.squareup.picasso.Picasso
import jp.dip.utb.imoyokan.*

class ThreadInfo(val url: String, mail: String) {
    @Suppress("MemberVisibilityCanBePrivate")
    val server: String
    @Suppress("MemberVisibilityCanBePrivate")
    val b: String
    val res: String
    var form = FromParams()
    var catalogImage: Bitmap? = null
    var thumbUrl: String = ""
    var imageUrl: String = ""
    var replies = ArrayList<ResInfo>()
    var exception: Exception? = null
    val isFailed: Boolean
        get() { return exception != null}
    val message: String
        get() { return exception?.message ?: ""}
    //val jsonUrl = base + "/futaba.php?mode=json&res=" + res + "&start=" + start + "&" + Math.random()

    init {
        val m = analyseUrl(url)
        this.server = m?.first ?: ""
        this.b = m?.second ?: ""
        this.res = m?.third ?: ""
        this.form.mail = mail
        if (m == null) {
            exception = Exception("URLが変！")
        }
    }

    fun getCatalogUrl(sort:String = ""): String {
        return "$server/$b/futaba.php?mode=cat${aroundWhenIsNotEmpty("&sort=", sort, "")}"
    }
}

class FromParams {
    var ptua = ""
    var mail = ""
}

class ResInfo(val index: Int, val number: String, val text: String, val mail: String = "") {
    /** 通知領域は狭いので適当に改行を抜く */
    fun getCompressText(): String {
        val levelRegex = "^(>*)".toRegex()
        val s = StringBuilder()
        var prevLevel = 0
        text.split("\n").forEach {
            val level = levelRegex.find(it)?.value?.length ?: 0
            if (prevLevel == level) {
                s.append(it.replace(levelRegex, " "))
            } else if (prevLevel == 0 || level == 0) {
                s.append("\n")
                s.append(it)
            } else {
                s.append(" ")
                s.append(it)
            }
            prevLevel = level
        }
        return s.toString().trim()
    }
}

class ThreadInfoBuilder {
    var url: String = ""
    var mail: String = ""

    fun build(): ThreadInfo {
        val threadInfo = ThreadInfo(url, mail)
        if (threadInfo.isFailed) {
            return threadInfo
        }

        // HTML読み込み
        val (_, _, result) = url.toHttps().httpGet().responseString(FUTABA_CHARSET)
        if (result is Result.Failure) {
            threadInfo.exception = result.getException()
            return threadInfo
        }
        val html = result.get()

        var index = 0
        var resNumber = ""
        var resMail = ""
        var isPre = true
        val threadMarker = "<input type=checkbox name=\"${threadInfo.res}\""
        val numberRegex =  "<input type=checkbox name=\"(\\d+)\"".toRegex()
        val mailRegex =  "<a href=\"mailto:([^\"]+)\">".toRegex()
        val textRegex =  "<blockquote[^>]*>([^\n]+)</blockquote>".toRegex()
        for (line in html.split("\n")) {
            if (isPre) {
                // フォームデータ
                if (line.contains("name=\"ptua\"")) {
                    threadInfo.form.ptua = line.pick("name=\"ptua\" value=\"(\\d+)\"".toRegex())
                    continue
                }
                if (line.contains(threadMarker)) {
                    // スレ画読み込み
                    val m = "<a href=\"/(${threadInfo.b}/src/[^\"]+)\" target=\"_blank\"><img src=\"/(${threadInfo.b}/thumb/\\d+s\\.jpg)".toRegex().find(line)
                    if (m != null) {
                        threadInfo.imageUrl = "${threadInfo.server}/${m.groupValues[1]}"
                        threadInfo.thumbUrl = "${threadInfo.server}/${m.groupValues[2]}"
                        threadInfo.catalogImage = Picasso.get().load(threadInfo.thumbUrl.replace("/thumb/", "/cat/").toHttps()).get()
                    }
                    resNumber = threadInfo.res
                    resMail = line.pick(mailRegex)
                    isPre = false
                    continue
                }
            }
            // レス
            if (line.contains("<blockquote")) {
                val resText = line.pick(textRegex)
                threadInfo.replies.add(ResInfo(index, resNumber, resText.removeHtmlTag(), resMail))
                index ++
                continue
            }
            // 番号とメール
            if (line.startsWith("<input type=checkbox")) {
                resNumber = line.pick(numberRegex)
                resMail = line.pick(mailRegex)
                continue
            }
        }

        return threadInfo
    }
}