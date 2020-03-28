package jp.dip.utb.imoyokan

import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager.GET_META_DATA
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.*
import java.lang.Long.max
import java.lang.Thread.sleep
import java.net.URL
import javax.net.ssl.HttpsURLConnection

class MainActivity : AppCompatActivity(), SharedPreferences.OnSharedPreferenceChangeListener {

    private lateinit var pref: Pref

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        pref = Pref.getInstance(applicationContext)
        refresh()
        if (pref.updateCheck) {
            updateCheck()
        }
    }

    // ボタンを出したり消したり
    override fun onResume() {
        super.onResume()
        pref.pref.registerOnSharedPreferenceChangeListener(this)
        refresh()
    }

    override fun onPause() {
        super.onPause()
        pref.pref.unregisterOnSharedPreferenceChangeListener(this)
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String) {
        pref.cache.remove(key)
        refresh()
    }

    private fun refresh() {
        top_message.visibility = visibleOrGone(pref.lastCatalogUrl.isEmpty() && pref.lastThreadUrl.isEmpty())
        catalog_button.visibility = visibleOrGone(pref.lastCatalogUrl.isNotEmpty())
        thread_button.visibility = visibleOrGone(pref.lastThreadUrl.isNotEmpty())
    }

    private fun autoFinish(delayMills: Long = 200L) {
        GlobalScope.launch {
            @Suppress("BlockingMethodInNonBlockingContext")
            sleep(delayMills)
            finish()
        }
    }

    // ボタンクリックイベント
    fun onClickLastThread(@Suppress("UNUSED_PARAMETER") view: View) {
        val intent = Intent(applicationContext, HiddenActivity::class.java)
        intent.putExtra(KEY_EXTRA_URL, pref.lastThreadUrl)
        startActivity(intent)
        autoFinish()
    }

    fun onClickLastCatalog(@Suppress("UNUSED_PARAMETER") view: View) {
        val intent = Intent(applicationContext, HiddenActivity::class.java)
        intent.putExtra(KEY_EXTRA_URL, pref.lastCatalogUrl)
        startActivity(intent)
        autoFinish()
    }

    fun onClickSettings(@Suppress("UNUSED_PARAMETER") view: View) {
        startActivity(Intent(this, SettingsActivity::class.java))
    }

    fun onClickUpdate(@Suppress("UNUSED_PARAMETER") view: View) {
        val uri = Uri.parse(getString(R.string.home_page_url))
        val intent = Intent(Intent.ACTION_VIEW, uri)
        startActivity(intent)
    }

    private fun updateCheck() {
        val handler = Handler()
        GlobalScope.launch { withContext(Dispatchers.IO) {
            try {
                val uri = URL(getString(R.string.apk_url))
                val conn = uri.openConnection() as HttpsURLConnection
                val packageInfo =
                    packageManager.getPackageInfo(applicationContext.packageName, GET_META_DATA)
                val lastUpdateTime =
                    max(packageInfo.lastUpdateTime, packageInfo.firstInstallTime)
                val lastModified = conn.getHeaderFieldDate("Last-Modified", lastUpdateTime)
                val hasUpdate = lastUpdateTime < lastModified
                handler.post {
                    update_button.visibility = if (hasUpdate) VISIBLE else GONE
                }
            } catch (e: Throwable) {
                Log.e(NOTIFY_NAME, "アプリの更新チェックに失敗しました", e)
            }
        } }
    }

}
