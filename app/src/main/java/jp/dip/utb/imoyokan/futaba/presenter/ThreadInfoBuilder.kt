package jp.dip.utb.imoyokan.futaba.presenter

import jp.dip.utb.imoyokan.futaba.model.ResInfo
import jp.dip.utb.imoyokan.futaba.model.ThreadInfo
import jp.dip.utb.imoyokan.futaba.util.*
import jp.dip.utb.imoyokan.util.*
import kotlin.collections.ArrayList

class ThreadInfoBuilder {
    var url: String = ""

    fun build(): ThreadInfo {
        val threadInfo = ThreadInfo(url)
        if (threadInfo.failedMessage.isNotBlank()) {
            return threadInfo
        }

        // HTML読み込み
        val res = HttpRequest(url).get()
        threadInfo.lastModified = res.header("last-modified") ?: ""
        threadInfo.statusCode = res.code()
        if (threadInfo.statusCode != 200) {
            threadInfo.failedMessage = "${res.code()} ${res.message()}"
            return threadInfo
        }
        val html = res.bodyString(FUTABA_CHARSET)
        var index = 0
        var resNumber = ""
        var resMail = ""
        var resDeleted = false
        var isPre = true
        var tryOldType = true
        val threadMarkerOld = "<input type=checkbox name=\"${threadInfo.res}\""
        val threadMarker = "<span id=\"delcheck${threadInfo.res}\""
        val numberRegexOld =  "<input type=checkbox name=\"(\\d+)\"".toRegex()
        val numberRegex = "<span id=\"delcheck(\\d+)".toRegex()
        val mailRegex =  "<a href=\"mailto:([^\"]+)\">".toRegex()
        val textRegex =  "<blockquote[^>]*>(.+)</blockquote>".toRegex()
        val resImageRegex = "<a href=\"/${threadInfo.b}/src/(\\d+$IMAGE_EXT)".toRegex()
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
            if (tryOldType && line.startsWith("<input type=checkbox")) {
                resNumber = line.pick(numberRegexOld)
                resMail = line.pick(mailRegex)
                continue
            }
            // 番号とメール
            if (line.contains("<span id=\"delcheck")) {
                resNumber = line.pick(numberRegex)
                resMail = line.pick(mailRegex)
                resDeleted = line.startsWith("<table border=0 class=deleted>")
                tryOldType = false
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
                        threadInfo.imageUrls.put(
                            getSiokaraUrl(
                                it.value
                            )
                        )
                    }
                }
                threadInfo.replies.add(
                    ResInfo(
                        index,
                        resNumber,
                        resText.removeHtmlTag(),
                        resMail,
                        resDeleted
                    )
                )
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