package jp.dip.utb.imoyokan

import android.content.Context
import androidx.preference.PreferenceManager
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty


class Pref private constructor(context: Context) {

    // シングルトン
    companion object {
        private var instance: Pref? = null
        fun getInstance(context: Context) = instance ?: synchronized(this) {
            instance ?: Pref(context).also { instance = it }
        }
    }

    // プロパティ
    var lastCatalogUrl: String by prefValue("last_catalog_url", "")
    var lastThreadUrl: String by prefValue("last_thread_url", "")
    var lastThreadModified: String by prefValue("last_thread_last_modified", "")
    val thread = Thread(this)
    val catalog = Catalog(this)

    class Thread(pref: Pref) {
        var shortKitaa: Boolean by pref.prefValue("thread_short_kitaa", false)
        var autoSmallFont: Boolean by pref.prefValue("thread_auto_small_font", false)
    }

    class Catalog(pref: Pref) {
        var cols: Int by pref.prefValue("catalog_cols", 7)
        var rows: Int by pref.prefValue("catalog_rows", 4)
        var sort: String by pref.prefValue("catalog_sort", "")
    }

    // Utils
    val pref = PreferenceManager.getDefaultSharedPreferences(context)!!
    val cache = mutableMapOf<String, Any>()
    private val modified = mutableMapOf<String, Any>()

    private fun <T: Any> prefValue(id: String, default: T) = object : ReadWriteProperty<Any, T> {
        override fun getValue(thisRef: Any, property: KProperty<*>): T {
            @Suppress("UNCHECKED_CAST")
            (cache[id] as? T)?.also { return it }
            @Suppress("IMPLICIT_CAST_TO_ANY")
            val value = when (default) {
                is Int -> pref.getString(id, default.toString())?.toInt() // getIntは使い物にならないのでStringで保存するしかない
                is String -> pref.getString(id, default)
                is Boolean -> pref.getBoolean(id, default)
                else -> default
            }
            cache[id] = value as Any
            @Suppress("UNCHECKED_CAST")
            return value as T
        }

        override fun setValue(thisRef: Any, property: KProperty<*>, value: T) {
            if (cache[id] != value) {
                cache[id] = value
                modified[id] = value
            }
        }
    }

    /** 設定を保存する */
    fun apply() {
        if (modified.isEmpty()) {
            return
        }
        val e = pref.edit()
        modified.forEach {
            when (it.value) {
                is Int -> e.putString(it.key, (it.value as Int).toString())
                is String -> e.putString(it.key, it.value as String)
                is Boolean -> e.putBoolean(it.key, it.value as Boolean)
                else -> { }
            }
        }
        e.apply()
        modified.clear()
    }
}
