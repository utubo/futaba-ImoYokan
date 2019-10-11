package jp.dip.utb.imoyokan.futaba

import android.graphics.Bitmap
import com.github.kittinunf.fuel.httpGet
import com.github.kittinunf.result.Result
import com.squareup.picasso.Picasso
import jp.dip.utb.imoyokan.aroundWhenIsNotEmpty
import jp.dip.utb.imoyokan.removeHtmlTag
import jp.dip.utb.imoyokan.toHttps

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

class ResInfo(val index: Int, val number: String, val text: String) {
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

        // スレ画読み込み
        val thumbUrl = "/thumb/(\\d+s\\.jpg)".toRegex().find(html)?.groupValues?.get(1)
        if (thumbUrl != null) {
            threadInfo.catalogImage = Picasso.get().load("${threadInfo.server}/${threadInfo.b}/cat/${thumbUrl}".toHttps()).get()
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