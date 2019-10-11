package jp.dip.utb.imoyokan.futaba

import com.github.kittinunf.fuel.httpGet
import com.github.kittinunf.fuel.httpPost
import com.github.kittinunf.result.Result
import jp.dip.utb.imoyokan.*

class Replyer {
    @ExperimentalUnsignedTypes
    fun reply(url: String, text: String, mail: String, ptua: String): Pair<String, String> {
        val (server, b, res) = analyseUrl(url)!!
        val (_, _, pthbResult) = "${server}/$URL_CACHEMT".toHttps().httpGet().responseString()
        val params = HashMap<String, String>()
        params["b"] = b
        params["resto"] = res
        params["com"] = text.replaceForPost(FUTABA_CHARSET)
        params["email"] = mail.replaceForPost(FUTABA_CHARSET)
        params["pwd"] = ""
        params["mode"] = "regist"
        params["ptua"] = ptua
        params["pthb"] = pthbResult.get().pick("return \"(\\d+)\"".toRegex())
        params["pthc"] = if (params["pthb"]!!.isNotEmpty()) (params["pthb"]!!.toULong() - RES_INTERVAL).toString() else ""
        params["baseform"] = ""
        params["js"] = "on"
        params["scsz"] = "1920x1080x24"
        params["chrenc"] = "文字"
        params["MAX_FILE_SIZE"] = "2048000"
        val body = StringBuilder()
        params.forEach {
            body.append("--").append(BOUNDARY).append("\r\n")
            body.append("Content-Disposition: form-data; name=\"${it.key}\"").append("\r\n")
            body.append("\r\n")
            body.append(it.value).append("\r\n")
        }
        body.append("--").append(BOUNDARY).append("--").append("\r\n")
        val (_, _, result) = "${server}/${b}/futaba.php?guid=on"
            .toHttps().httpPost()
            .header("User-Agent" to USER_AGENT)
            .header("Cookie" to "posttime=${params["pthc"]};")
            .header("Content-Type" to "multipart/form-data; boundary=$BOUNDARY")
            .body(body.toString(), FUTABA_CHARSET)
            .responseString(FUTABA_CHARSET)
        return when (result) {
            is Result.Failure -> {
                Pair("返信失敗", result.getException().message ?: "")
            }
            is Result.Success -> {
                val msg =
                    "<font color=red size=5><b>([^<]+)<br>".toRegex().find(result.get())?.value?.removeHtmlTag()
                        ?: "<body[^>]*>(.+)</body>".toRegex().find(result.get())?.value?.removeHtmlTag()
                        ?: ""
                Pair("返信しました", msg)
            }
        }
    }
}