package jp.dip.utb.imoyokan.futaba

import jp.dip.utb.imoyokan.*

class CatalogInfo(val url: String) {
    val items: ArrayList<CatalogItem> = ArrayList()
    val server: String
    val b: String
    val sort: String
    var failedMessage: String = ""
    val isFailed: Boolean
    get() { return failedMessage.isNotBlank()}

    init {
        val m = analyseCatalogUrl(url)
        this.server = m?.first ?: ""
        this.b = m?.second ?: ""
        this.sort = m?.third ?: ""
        if (m == null) {
            failedMessage = "URLが変！"
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

        try {
            // HTML読み込み
            val res = HttpRequest(url).header("Cookie", "cxyl=${cols}x${rows}x${textLength}x0x0;").get()
            if (res.code() != 200) {
                catalogInfo.failedMessage = res.message()
                return catalogInfo
            }
            val html = res.bodyString(FUTABA_CHARSET)

            // 解析
            if (html.contains("JSON.parse('{\"res\"")) {
                // JSONを解析(レイアウト板とか)そのうち全部この形式になるらしい
                val json = html.pick("JSON.parse\\('(.+)'\\);</script>")
                // 普通のJSONじゃないみたい…
                //val items = JSONObject(json).getJSONArray("res")
                val items = toMapArray(json)
                items.forEach {
                    if (it["no"] != null) {
                        val href =
                            "${catalogInfo.server}/${catalogInfo.b}/res/${it["no"]}.htm"
                        val img = if (it["src"] != null) "${catalogInfo.server}${it["src"]}".toHttps() else null
                        val text = it["com"]?.removeHtmlTag() ?: ""
                        val count = toInt(it["cr"])
                        val item = CatalogItem(href, img, text, count)
                        catalogInfo.items.add(item)
                    }
                }
            } else {
                // 旧版tableタグから解析(こっちはそのうちなくなるらしい)
                "<td><a href='(res/\\d+.htm)' target='_blank'><img src='/([^']+)'[^>]+></a>(.*)<font size=2>(\\d+)</font></td>".toRegex()
                    .findAll(html).forEach {
                    val href = "${catalogInfo.server}/${catalogInfo.b}/${it.groupValues[1]}"
                    val img = "${catalogInfo.server}/${it.groupValues[2]}".toHttps()
                    val text = it.groupValues[3].removeHtmlTag()
                    val count = it.groupValues[4].toInt()
                    val item = CatalogItem(href, img, text, count)
                    catalogInfo.items.add(item)
                }
            }
        } catch (t: Throwable) {
            catalogInfo.failedMessage = t.message.toString()
        }

        return catalogInfo
    }

    /**
     * JSONを雑に階層のない平坦なListにします<br>
     * (カタログで使いたいだけなので)
     */
    private fun toMapArray(json: String): List<Map<String, String>> {
        val maxIndex = json.length - 1
        var isInStr = false
        var key = ""
        val value = StringBuilder()
        var item = HashMap<String, String>()
        val items = ArrayList<HashMap<String, String>>()
        for (i in 0..maxIndex) {
            val c = json[i]
            if (isInStr) {
                when (c) {
                    '"' -> isInStr = false
                    '\\' -> { }
                    else -> value.append(c)
                }
                continue
            }
            when (c) {
                '{' -> {
                    item = HashMap()
                    items.add(item)
                }
                '"' -> {
                    isInStr = true
                    value.clear()
                }
                ':' -> {
                    key = value.toString()
                    value.clear()
                }
                ',', '}' -> {
                    if (key.isNotBlank()) item[key] = value.toString()
                    key = ""
                    value.clear()
                }
                else -> value.append(c)
            }
        }
        return items
    }

    private fun toInt(s: String?, default: Int = 0): Int {
        return try {
            if (s == null || s.isBlank()) default else Integer.parseInt(s)
        } catch (t: Throwable) {
            default
        }
    }

}