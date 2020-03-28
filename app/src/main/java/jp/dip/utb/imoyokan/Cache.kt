package jp.dip.utb.imoyokan

import android.content.Context
import android.util.Log
import jp.dip.utb.imoyokan.futaba.CatalogInfo
import jp.dip.utb.imoyokan.futaba.ThreadInfo
import java.io.*

class Cache(val context: Context) {

    companion object {
        private const val CACHE_FILENAME = "thread_cache.dat"
        private const val CATALOG_CACHE_FILENAME = "catalog_cache.dat"
    }

    @Suppress("UNCHECKED_CAST")
    fun <T> load(filename: String): T? {
        try {
            val file = File(context.cacheDir, filename)
            ObjectInputStream(FileInputStream(file)).use {
                (it.readObject() as? T)?.also { result -> return result }
            }
        } catch (e: Throwable) {
            Log.d(NOTIFY_NAME, "Failed to load from $filename.", e)
        }
        return null
    }

    fun <T> save(info: T, filename: String) {
        try {
            val file = File(context.cacheDir, filename)
            ObjectOutputStream(FileOutputStream(file)).use{ it.writeObject(info) }
        } catch (e: Throwable) {
            Log.d(NOTIFY_NAME, "Failed to save to $filename.", e)
        }
    }

    fun loadThreadInfo(): ThreadInfo? {
        return load(CACHE_FILENAME)
    }

    fun saveThreadInfo(threadInfo: ThreadInfo) {
        save(threadInfo, CACHE_FILENAME)
    }
    fun loadCatalogInfo(): CatalogInfo? {
        return load(CATALOG_CACHE_FILENAME)
    }

    fun saveCatalogInfo(catalogInfo: CatalogInfo) {
        save(catalogInfo, CATALOG_CACHE_FILENAME)
    }

}