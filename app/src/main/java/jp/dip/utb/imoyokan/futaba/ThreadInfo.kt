package jp.dip.utb.imoyokan.futaba

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.httpGet
import com.github.kittinunf.result.Result
import jp.dip.utb.imoyokan.*
import java.lang.StringBuilder

class ThreadInfo(val url: String, mail: String) {
    @Suppress("MemberVisibilityCanBePrivate")
    val server: String
    @Suppress("MemberVisibilityCanBePrivate")
    val b: String
    val res: String
    var form = FromParams()
    var catalogImage: Bitmap? = null
    var replies = ArrayList<ResInfo>()
    var mails = HashMap<String, String>()

    init {
        val (server, b, res) = analyseUrl(url)!!
        this.server = server
        this.b = b
        this.res = res
        this.form.mail = mail
    }
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
    var url: String = ""
    var mail: String = ""
    var cacheImg: Bitmap? = null

    fun build(): ThreadInfo {
        val threadInfo = ThreadInfo(url, mail)

        // HTML読み込み
        val (_, _, result) = url.toHttps().httpGet().responseString(FUTABA_CHARSET)
        var exception: Exception? = null
        val html = when (result) {
            is Result.Success -> result.get()
            is Result.Failure -> {  exception = result.getException(); "" }
        }
        if (exception != null) {
            threadInfo.replies.add(ResInfo(0, threadInfo.res, "スレッド取得失敗${aroundWhenIsNotEmpty("\n", exception.message, "")}"))
            return threadInfo
        }

        // スレ画読み込み
        if (cacheImg != null) {
            threadInfo.catalogImage = cacheImg
        } else {
            val thumbUrl = "/thumb/(\\d+s\\.jpg)".toRegex().find(html)?.groupValues?.get(1)
            if (thumbUrl != null) {
                val jpegBinary =
                    Fuel.download("${threadInfo.server}/${threadInfo.b}/cat/${thumbUrl}".toHttps()).response().second.data
                threadInfo.catalogImage =
                    BitmapFactory.decodeByteArray(jpegBinary, 0, jpegBinary.size)
            }
        }

        // フォームデータ
        threadInfo.form.ptua = "name=\"ptua\" value=\"(\\d+)\"".toRegex().find(html)?.groupValues?.get(1) ?: ""

        // スレ本文(スレ本文はblockquoteの前に改行がある)
        var index = 0
        val text: String = "<blockquote>([^\\n]+)</blockquote>".toRegex().find(html)?.groupValues?.get(1)?.removeHtmlTag() ?: ""
        threadInfo.replies.add(ResInfo(index, threadInfo.res, text))

        // レス
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