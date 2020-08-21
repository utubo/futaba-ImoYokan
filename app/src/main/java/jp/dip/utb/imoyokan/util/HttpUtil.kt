package jp.dip.utb.imoyokan.util

import jp.dip.utb.imoyokan.futaba.util.USER_AGENT
import okhttp3.*
import java.net.URLEncoder
import java.nio.charset.Charset

// OKHttpはシングルトンがいいらしい
class OKHttpSingleton private constructor() {
    companion object {
        private var instance: OKHttpSingleton? = null
        fun getSingleton() = instance
            ?: synchronized(this) {
            instance
                ?: OKHttpSingleton()
                    .also { instance = it }
        }
    }
    var client: OkHttpClient = OkHttpClient.Builder().build()
}

// エンコード指定でbodyを取れるように
fun Response.bodyString(charset: Charset? = null): String {
    val body = this.body()
    return if (body == null) "" else if (charset == null) body.toString() else String(body.bytes(), charset)
}

// ↑これだけだと非常に面倒なので↓ユーティリティも定義しておく

class HttpRequest(val url: String) {
    private val request = Request.Builder().url(url).header("User-Agent",
        USER_AGENT
    )
    fun header(name: String, value: String): HttpRequest {
        request.header(name, value)
        return this
    }
    fun get(): Response {
        return OKHttpSingleton.getSingleton().client.newCall(request.build()).execute()
    }
    fun post(params: HashMap<String, String>, charset: Charset): Response {
        val formBuilder = FormBody.Builder()
        val charsetName = charset.name()
        params.forEach { (key, value) -> formBuilder.addEncoded(key, URLEncoder.encode(value, charsetName)) }
        return OKHttpSingleton.getSingleton().client.newCall(request.post( formBuilder.build()).build()).execute()
    }
}
