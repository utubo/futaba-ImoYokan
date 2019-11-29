package jp.dip.utb.imoyokan.futaba

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
    //↓JSONデータはスレ本文がないので使いにくい
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
        return failedMessage.isNotBlank() || replies.isEmpty()
    }

}

data class FromParams(var ptua: String, var mail: String) : Serializable

data class ResInfo(val index: Int, val number: String, val text: String, val mail: String = "") : Serializable

class ThreadInfoBuilder {
    var url: String = ""
    var mail: String = ""

    fun build(): ThreadInfo {
        val threadInfo = ThreadInfo(url).apply { this.form.mail = mail }
        if (threadInfo.isFailed()) {
            return threadInfo
        }

        // HTML読み込み
        val res = HttpRequest(url).get()
        if (res.code() != 200) {
            threadInfo.failedMessage = res.message()
            return threadInfo
        }
        val html = res.bodyString(FUTABA_CHARSET)
        threadInfo.lastModified = res.header("last-modified").toString()

        var index = 0
        var resNumber = ""
        var resMail = ""
        var isPre = true
        val threadMarkerOld = "<input type=checkbox name=\"${threadInfo.res}\""
        val threadMarker = "<span id=\"delcheck${threadInfo.res}\""
        val numberRegexOld =  "<input type=checkbox name=\"(\\d+)\"".toRegex()
        val numberRegex = "<span id=\"delcheck(\\d+)".toRegex()
        val mailRegex =  "<a href=\"mailto:([^\"]+)\">".toRegex()
        val textRegex =  "<blockquote[^>]*>(.+)</blockquote>".toRegex()
        val resImageRegex = "<a href=\"/${threadInfo.b}/src/(\\d+${IMAGE_EXT})".toRegex()
        for (line in html.split("\n")) {
            if (isPre) {
                // フォームデータ
                if (line.contains("name=\"ptua\"")) {
                    threadInfo.form.ptua = line.pick("name=\"ptua\" value=\"(\\d+)\"".toRegex())
                    continue
                }
                if (line.contains(threadMarker)  || line.contains(threadMarkerOld)) {
                    // スレ画読み込み(板によってダブルクォーテーションだったりシングルクォーテーションだったりする…)
                    "<a href=./(${threadInfo.b}/src/[^\"']+). target=._blank.><img src=./${threadInfo.b}/thumb/\\d+s\\.jpg.".toRegex().find(line)?.forGroupValue(1) {
                        threadInfo.imageUrls.put("${threadInfo.server}/${it}")
                    }
                    resNumber = threadInfo.res
                    resMail = line.pick(mailRegex)
                    isPre = false
                    continue
                }
            }
            // 番号とメール(旧)
            if (line.startsWith("<input type=checkbox")) {
                resNumber = line.pick(numberRegexOld)
                resMail = line.pick(mailRegex)
                continue
            }
            // 番号とメール
            if (line.startsWith("<span id=\"delcheck")) {
                resNumber = line.pick(numberRegex)
                resMail = line.pick(mailRegex)
            }
            // レス
            if (line.contains("<blockquote")) {
                var resText = line.pick(textRegex)
                if (IMAGE_EXT_REGEX.containsMatchIn(line)) {
                    resImageRegex.find(line)?.forGroupValue(1) {
                        resText = "${it}\n${resText}"
                        threadInfo.imageUrls.put("${threadInfo.server}/${threadInfo.b}/src/${it}")
                    }
                    SIO_FILE_REGEX.findAll(resText).forEach {
                        threadInfo.imageUrls.put(getSiokaraUrl(it.value))
                    }
                }
                threadInfo.replies.add(ResInfo(index, resNumber, resText.removeHtmlTag(), resMail))
                index ++
                continue
            }
        }
        return threadInfo
    }

    private fun <T> ArrayList<T>.put(v: T) {
        if (!this.contains(v)) this.add(v)
    }
}