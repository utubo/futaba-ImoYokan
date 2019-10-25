package jp.dip.utb.imoyokan

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.lang.Thread.sleep


class MainActivity : AppCompatActivity(), SharedPreferences.OnSharedPreferenceChangeListener {

    private lateinit var pref: Pref

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        pref = Pref.getInstance(applicationContext)
        refresh()
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

}
