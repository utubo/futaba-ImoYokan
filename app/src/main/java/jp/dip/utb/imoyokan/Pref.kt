package jp.dip.utb.imoyokan

import android.annotation.SuppressLint
import android.content.Context
import androidx.preference.PreferenceManager
import java.text.SimpleDateFormat
import java.util.*
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty


class Pref private constructor(context: Context) {

    companion object {
        // シングルトン
        var instance: Pref? = null
        fun getInstance(context: Context) = instance ?: synchronized(this) {
            instance ?: Pref(context.applicationContext).also { instance = it }
        }
    }

    // プロパティ
    var lastUrl: String by prefValue("last_url", "")
    var lastCatalogUrl: String by prefValue("last_catalog_url", "")
    var lastThreadUrl: String by prefValue("last_thread_url", "")
    var lastThreadModified: String by prefValue("last_thread_last_modified", "")
    val thread = Thread(this)
    val catalog = Catalog(this)
    val media = Media(this)
    val mail = Mail(this)
    var confirmBeforeReply: Boolean by prefValue("reply_confirm", true)
    var reverseScrolling: Boolean by prefValue("reverse_scrolling", false)
    var updateCheck: Boolean by prefValue("update_check", true)
    val debugMode: Boolean by prefValue("debug_mode", false)

    class Thread(pref: Pref) {
        var removeLineBreaks: Boolean by pref.prefValue("thread_remove_line_breaks", true)
        var shortKitaa: Boolean by pref.prefValue("thread_short_kitaa", false)
        var autoSmallFont: Boolean by pref.prefValue("thread_auto_small_font", false)
        var fontSize: Float by pref.prefValue("thread_font_size", 1.0f)
        var showDeleted: Boolean by pref.prefValue("thread_show_deleted", false)
    }

    class Catalog(pref: Pref) {
        var cols: Int by pref.prefValue("catalog_cols", 7)
        var rows: Int by pref.prefValue("catalog_rows", 4)
        var sort: String by pref.prefValue("catalog_sort", "")
        var enableScrolling: Boolean by pref.prefValue("catalog_enable_scrolling", true)
    }

    class Media(pref: Pref) {
        var useSioCacheServer: Boolean by pref.prefValue("media_use_sio_cache_server", true)
    }

    class Mail(pref: Pref) {
        var ignoreWideChar: Boolean by pref.prefValue("mail_ignore_wide_char", false)
        var keepHours: Int by pref.prefValue("mail_keep_hours", 1)
        var lastNoBlank: String by pref.prefValue("mail_no_blank", "")
        private var last: String by pref.prefValue("mail_last", "")
        private var timestamp: String by pref.prefValue("mail_timestamp", "")
        private var url: String by pref.prefValue("mail_url", "")

        fun set(mail:String, url: String) {
            if (mail.isNotBlank()) {
                lastNoBlank = mail
                timestamp = yyyyMMddHHmmss(Date())
            }
            last = mail
            this.url = url
        }

        fun get(url: String): String {
            if (url == this.url) {
                return last
            }
            if (keepHours < 0) {
                return last
            }
            if (keepHours == 0) {
                return ""
            }
            if (border()< timestamp) {
                return last
            }
            return ""
        }

        private fun border(): String {
            val border = Calendar.getInstance()
            border.add(Calendar.HOUR_OF_DAY, -keepHours)
            return yyyyMMddHHmmss(border.time)
        }
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
                is Float -> pref.getString(id, default.toString())?.toFloat()
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
                is Float -> e.putString(it.key, (it.value as Float).toString())
                else -> { }
            }
        }
        e.apply()
        modified.clear()
    }

}

@SuppressLint("SimpleDateFormat")
private fun yyyyMMddHHmmss(date: Date): String {
    return SimpleDateFormat("yyyy/MM/dd HH:mm:ss").format(date)
}
