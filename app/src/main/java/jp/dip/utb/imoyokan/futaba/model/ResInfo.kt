package jp.dip.utb.imoyokan.futaba.model

import java.io.Serializable

data class ResInfo(
    val index: Int,
    val number: String,
    val text: String,
    val mail: String = "",
    val deleted: Boolean = false
) : Serializable