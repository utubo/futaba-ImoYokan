package jp.dip.utb.imoyokan

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager


class Pref private constructor(context: Context) {

    companion object {
        private var instance: Pref? = null
        fun getInstance(context: Context) = instance ?: synchronized(this) {
            instance ?: Pref(context).also { instance = it }

        }
    }

    var lastCatalogUrl: String
        get() = getX("last_catalog_url", "")
        set(v) { putX("last_catalog_url", v)}

    var lastThreadUrl: String
        get() = getX("last_thread_url", "")
        set(v) { putX("last_thread_url", v)}

    val catalog = Catalog(this)

    class Catalog(private val pref: Pref) {
        var cols: Int
            get() = pref.getX("catalog_cols", 7)
            set(v) { pref.putX("catalog_cols", v)}
        var rows: Int
            get() = pref.getX("catalog_rows", 4)
            set(v) { pref.putX("catalog_rows", v)}
        var sort: String
            get() = pref.getX("catalog_sort", "")
            set(v) { pref.putX("catalog_sort", v)}
    }

    // Utils
    val pref: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
    val cache = mutableMapOf<String, Any>()
    private val modified = mutableMapOf<String, Any>()

    private inline fun <reified T> getX(id: String, default: T): T {
        val v = cache[id]
        if (v != null && v is T) return v
        @Suppress("IMPLICIT_CAST_TO_ANY")
        val v1 = when (default) {
            is Int -> pref.getString(id, default.toString())?.toInt() // getIntは使い物にならないのでStringで保存するしかない
            is String -> pref.getString(id, default)
            else -> default
        }
        cache[id] = v1 as Any
        return v1 as T
    }

    private fun putX(id: String, value: Any) {
        if (cache[id] != value) {
            cache[id] = value
            modified[id] = value
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
                else -> { }
            }
        }
        e.apply()
        modified.clear()
    }
}
