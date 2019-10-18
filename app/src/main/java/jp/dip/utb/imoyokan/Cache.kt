package jp.dip.utb.imoyokan

import android.content.Context
import android.util.Log
import jp.dip.utb.imoyokan.futaba.ThreadInfo
import java.io.*

class Cache(private val context: Context) {

    companion object {
        private const val CACHE_FILENAME = "thread_cache.dat"
    }

    fun loadFromCache(): ThreadInfo? {
        try {
            val file = File(context.cacheDir, CACHE_FILENAME)
            ObjectInputStream(FileInputStream(file)).use {
                (it.readObject() as? ThreadInfo)?.also { result -> return result }
            }
        } catch (e: Throwable) {
            Log.d(NOTIFY_NAME, "Failed to load thread cache.", e)
        }
        return null
    }

    fun saveToCache(threadInfo: ThreadInfo) {
        try {
            val file = File(context.cacheDir, CACHE_FILENAME)
            ObjectOutputStream(FileOutputStream(file)).use{ it.writeObject(threadInfo) }
        } catch (e: Throwable) {
            Log.d(NOTIFY_NAME, "Failed to save thread cache.", e)
        }
    }

}