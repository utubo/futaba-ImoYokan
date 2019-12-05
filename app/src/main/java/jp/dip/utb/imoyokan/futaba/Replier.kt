package jp.dip.utb.imoyokan.futaba

import jp.dip.utb.imoyokan.*

class Replier {
    @ExperimentalUnsignedTypes
    fun reply(url: String, text: String, mail: String, ptua: String): Pair<String, String> {
        val (server, b, resto) = analyseUrl(url) ?: return Pair("返信失敗", "URLが変！\n${url}")
        val pthb = HttpRequest("${server}/$URL_CACHEMT".toHttps()).get().bodyString(FUTABA_CHARSET).pick("return \"(\\d+)\"")
        val params = HashMap<String, String>()
        params["b"] = b
        params["resto"] = resto
        params["com"] = text.replaceForPost(FUTABA_CHARSET)
        params["email"] = mail.replaceForPost(FUTABA_CHARSET)
        params["pwd"] = ""
        params["mode"] = "regist"
        params["ptua"] = ptua
        params["pthb"] = pthb
        params["pthc"] = if (pthb.isNotEmpty()) (pthb.toULong() - RES_INTERVAL).toString() else ""
        params["baseform"] = ""
        params["js"] = "on"
        params["scsz"] = "1920x1080x24"
        params["chrenc"] = "文字"
        params["MAX_FILE_SIZE"] = "2048000"
        val res = HttpRequest("${server}/${b}/futaba.php?guid=on".toHttps())
            .header("Cookie", "posttime=${params["pthc"]};")
            .post(params, FUTABA_CHARSET)
        return if (res.code() != 200) {
            Pair("返信失敗", res.message())
        } else {
            val html = res.bodyString(FUTABA_CHARSET)
            val msg =
                "<font color=red size=5><b>([^<]+)<br>".toRegex().find(html)?.value?.removeHtmlTag()
                    ?: "<bodyString[^>]*>(.+)</bodyString>".toRegex().find(html)?.value?.removeHtmlTag()
                    ?: ""
            Pair("返信しました", msg)
        }
    }
}