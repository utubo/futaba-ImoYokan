package jp.dip.utb.imoyokan.futaba

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.httpGet
import com.github.kittinunf.result.Result
import jp.dip.utb.imoyokan.*
import java.lang.StringBuilder

class ThreadInfo(val url: String, @Suppress("unused") val server: String, @Suppress("unused") val b: String, val res: String, val text: String) {
    var catalogImage: Bitmap? = null
    var replies = ArrayList<ResInfo>()
    var mails = HashMap<String, String>()
    var form = FromParams()
}
class FromParams {
    var ptua = ""
    var mail = ""
}
class ResInfo(val index: Int, val number: String, val text: String) {
    /** 通知領域は狭いので適当に改行を抜く */
    val compressText: String
    get() {
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
    fun analyseUrl(url: String): Triple<String, String, String>? {
        val urlMatches = "(https?://.*\\.2chan\\.net)/([^/]+)/res/(\\d+).htm".toRegex().find(url)?.groupValues ?: return null
        val server = urlMatches[1]
        val b: String = urlMatches[2]
        val res = urlMatches[3]
        return Triple(server, b, res)
    }

    fun build(url: String , mail: String? = null): ThreadInfo {
        val (server, b, res) = analyseUrl(url)!!
        val (_, _, result) = url.toHttps().httpGet()
            .responseString(FUTABA_CHARSET)
        var exception: Exception? = null
        val html = when (result) {
            is Result.Success -> result.get()
            is Result.Failure -> {  exception = result.getException(); "" }
        }

        val text: String? = "<blockquote>([^\\n]+)</blockquote>".toRegex().find(html)?.groupValues?.get(1)?.removeHtmlTag()
        val threadInfo =
            ThreadInfo(url, server, b, res, text ?: "スレッド取得失敗${exception?.message?.around(" ", "") ?: ""}")
        // 必須情報ここまで

        // サムネ
        val thumbUrl= "/thumb/(\\d+s\\.jpg)".toRegex().find(html)?.groupValues?.get(1)
        if (thumbUrl != null) {
            val jpegBinary = Fuel.download("${server}/${b}/cat/${thumbUrl}").response().second.data
            threadInfo.catalogImage = BitmapFactory.decodeByteArray(jpegBinary, 0, jpegBinary.size)
        }
        // フォームデータ
        if (mail != null) {
            threadInfo.form.mail = mail
        }
        threadInfo.form.ptua = "name=\"ptua\" value=\"(\\d+)\"".toRegex().find(html)?.groupValues?.get(1) ?: ""
        // レス
        var index = 0
        if (text != null) {
            threadInfo.replies.add(ResInfo(index, res, text))
        }
        "id=sd(\\d+)>.*</a><blockquote[^>]*>([^\\n]+)</blockquote>".toRegex().findAll(html).forEach {
            index ++
            threadInfo.replies.add(
                ResInfo(
                    index,
                    it.groupValues[1],
                    it.groupValues[2].removeHtmlTag()
                )
            )
        }
        // メールアドレス
        "id=delcheck(\\d+)><a href=\"mailto:([^\"]+)\">".toRegex().findAll(html).forEach {
            threadInfo.mails[it.groupValues[1]] = it.groupValues[2].removeHtmlTag()
        }
        return threadInfo
    }
}