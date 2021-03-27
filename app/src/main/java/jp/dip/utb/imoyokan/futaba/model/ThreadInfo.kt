package jp.dip.utb.imoyokan.futaba.model

import jp.dip.utb.imoyokan.futaba.util.analyseUrl
import java.io.Serializable
import java.util.*
import kotlin.collections.ArrayList

data class ThreadInfo(val url: String) : Serializable {
    val server: String
    val b: String
    val res: String
    var form = FormParams()
    var replies =
        ArrayList<ResInfo>()
    val timestamp = Date()
    var lastModified = ""
    var statusCode = 200
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