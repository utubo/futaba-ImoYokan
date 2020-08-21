package jp.dip.utb.imoyokan.futaba.model

import java.io.Serializable

data class CatalogItem (
    val href: String,
    val img: String?,
    val text: String,
    @Suppress("unused")
    val count: Int,
    var filtered: Boolean = false // For Imoyokan word filter.
) : Serializable