package jp.dip.utb.imoyokan.futaba

import com.github.kittinunf.fuel.httpGet
import com.github.kittinunf.result.Result
import jp.dip.utb.imoyokan.*
import java.util.*
import kotlin.collections.ArrayList
import java.io.Serializable

data class ThreadInfo(val url: String) : Serializable {
    val server: String
    val b: String
    val res: String
    var form = FromParams("", "")
    var replies = ArrayList<ResInfo>()
    var timestamp = Date()
    var lastModified = ""
    var failedMessage = ""
    var imageUrls = ArrayList<String>()
    //val jsonUrl = base + "/futaba.php?mode=json&res=" + res + "&start=" + start + "&" + Math.random()

    init {
        val m = analyseUrl(url)
        this.server = m?.first ?: ""
        this.b = m?.second ?: ""
        this.res = m?.third ?: ""
        if (m == null) {
            failedMessage = "URLが変！"
        }
    }

    fun isFailed(): Boolean {
        return failedMessage.isNotBlank()
    }

}

data class FromParams(var ptua: String, var mail: String) : Serializable

data class ResInfo(val index: Int, val number: String, val text: String, val mail: String = "") : Serializable {
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
        val threadInfo = ThreadInfo(url).apply { this.form.mail = mail }
        if (threadInfo.isFailed()) {
            return threadInfo
        }

        // HTML読み込み
        val (_, response, result) = url.toHttps().httpGet().responseString(FUTABA_CHARSET)
        if (result is Result.Failure) {
            threadInfo.failedMessage = result.getException().message.toString()
            return threadInfo
        }
        val html = result.get()
        threadInfo.lastModified = response.header("last-modified").toString()

        var index = 0
        var resNumber = ""
        var resMail = ""
        var isPre = true
        val threadMarker = "<input type=checkbox name=\"${threadInfo.res}\""
        val numberRegex =  "<input type=checkbox name=\"(\\d+)\"".toRegex()
        val mailRegex =  "<a href=\"mailto:([^\"]+)\">".toRegex()
        val textRegex =  "<blockquote[^>]*>(.+)</blockquote>".toRegex()
        val imageRegex = "(sa|sp|sq|su|ss)?\\d+${IMAGE_EXT}".toRegex()
        val resImageRegex = "<a href=\"/${threadInfo.b}/src/(\\d+${IMAGE_EXT})".toRegex()
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
                        threadInfo.imageUrls.put("${threadInfo.server}/${m.groupValues[1]}")
                    }
                    resNumber = threadInfo.res
                    resMail = line.pick(mailRegex)
                    isPre = false
                    continue
                }
            }
            // レス
            if (line.contains("<blockquote")) {
                var resText = line.pick(textRegex)
                if (IMAGE_EXT_REGEX.containsMatchIn(line)) {
                    resImageRegex.find(line)?.let {
                        resText = "${it.groupValues[1]}\n${resText}"
                    }
                    imageRegex.findAll(resText).forEach {
                        when {
                            it.value.startsWith("sa") -> threadInfo.imageUrls.put(SIO_KARA_SA_ROOT + it.value)
                            it.value.startsWith("sp") -> threadInfo.imageUrls.put(SIO_KARA_SP_ROOT + it.value)
                            it.value.startsWith("sq") -> threadInfo.imageUrls.put(SIO_KARA_SQ_ROOT + it.value)
                            it.value.startsWith("ss") -> threadInfo.imageUrls.put(SIO_KARA_SS_ROOT + it.value)
                            it.value.startsWith("su") -> threadInfo.imageUrls.put(SIO_KARA_SU_ROOT + it.value)
                            else -> threadInfo.imageUrls.put("${threadInfo.server}/${threadInfo.b}/src/${it.value}")
                        }
                    }
                }
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

    private fun <T> ArrayList<T>.put(v: T) {
        if (!this.contains(v)) this.add(v)
    }
}