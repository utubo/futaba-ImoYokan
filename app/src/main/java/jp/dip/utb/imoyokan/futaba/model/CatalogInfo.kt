package jp.dip.utb.imoyokan.futaba.model

import jp.dip.utb.imoyokan.futaba.util.analyseCatalogUrl
import java.io.Serializable
import java.util.*
import kotlin.collections.ArrayList

data class CatalogInfo(val url: String): Serializable {
    val items: ArrayList<CatalogItem> =
        ArrayList()
    val server: String
    val b: String
    val sort: String
    val timestamp = Date()
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