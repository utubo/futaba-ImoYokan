package jp.dip.utb.imoyokan.futaba

import com.github.kittinunf.fuel.httpGet
import com.github.kittinunf.result.Result
import jp.dip.utb.imoyokan.pick
import jp.dip.utb.imoyokan.removeHtmlTag
import jp.dip.utb.imoyokan.toHttps
import org.json.JSONArray
import org.json.JSONObject

class CatalogInfo(val url: String) {
    val items: ArrayList<CatalogItem> = ArrayList()
    val server: String
    val b: String
    val sort: String
    var exception: Exception? = null
    val isFailed: Boolean
    get() { return exception != null}
    val message: String
    get() { return exception?.message ?: ""}

    init {
        val m = analyseCatalogUrl(url)
        this.server = m?.first ?: ""
        this.b = m?.second ?: ""
        this.sort = m?.third ?: ""
        if (m == null) {
            exception = Exception("URLが変！")
        }
    }
}

class CatalogItem(
    val href: String,
    val img: String?,
    val text: String,
    @Suppress("unused")
    val count: Int
)

class CatalogInfoBuilder(private val url: String, private val cols: Int = 7, private val rows: Int = 3, private val textLength: Int = 4) {

    private val catalogInfo = CatalogInfo(url)

    fun buildWithoutReload(): CatalogInfo {
        return catalogInfo
    }

    fun reload(): CatalogInfo {
        if (catalogInfo.isFailed) {
            return catalogInfo
        }

        // HTML読み込み
        val (_, _, result) = url.toHttps().httpGet().header("Cookie", "cxyl=${cols}x${rows}x${textLength}x0x0;") .responseString(FUTABA_CHARSET)
        if (result is Result.Failure) {
            catalogInfo.exception = result.getException()
            return catalogInfo
        }
        val html = result.get()

        // 解析
        if (html.contains("JSON.parse('{\"res\"")) {
            // JSONを解析(レイアウト板とか)そのうち全部この形式になるらしい
            // アプリサイズを大きくしたくないのでJSONは標準ライブラリで解析する
            val json = html.pick("JSON.parse\\('(.+)'\\);</script>")
            val items = JSONObject(json).getJSONArray("res")
            items.forEach {
                val href = "${catalogInfo.server}/${catalogInfo.b}/res/${it.getString("no")}.htm"
                val img = if (it.has("src")) "${catalogInfo.server}${it.getStringDefault("src")}".toHttps() else null
                val text = it.getStringDefault("com").removeHtmlTag()
                val count = it.getInt("cr")
                val item = CatalogItem(href, img, text, count)
                catalogInfo.items.add(item)
            }
        } else {
            // 旧版tableタグから解析(こっちはそのうちなくなるらしい)
            "<td><a href='(res/\\d+.htm)' target='_blank'><img src='/([^']+)'[^>]+></a>(.*)<font size=2>(\\d+)</font></td>".toRegex().findAll(html).forEach {
                val href = "${catalogInfo.server}/${catalogInfo.b}/${it.groupValues[1]}"
                val img = "${catalogInfo.server}/${it.groupValues[2]}".toHttps()
                val text = it.groupValues[3].removeHtmlTag()
                val count = it.groupValues[4].toInt()
                val item = CatalogItem(href, img, text, count)
                catalogInfo.items.add(item)
            }
        }

        return catalogInfo
    }

    private fun JSONArray.forEach(action: (JSONObject) -> Unit) {
        for (i in 0 until length()) {
            action(this.getJSONObject(i))
        }
    }

    private fun JSONObject.getStringDefault(key: String, default: String = ""): String {
        return if (this.has(key)) this.getString(key).replace("\\", "") else default // アンエスケープ面倒だから…
    }
}