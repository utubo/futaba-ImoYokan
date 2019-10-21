package jp.dip.utb.imoyokan

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity


class MainActivity : AppCompatActivity(), SharedPreferences.OnSharedPreferenceChangeListener {

    private lateinit var pref: Pref
    private lateinit var topMessage: TextView
    private lateinit var catalog: TextView
    private lateinit var thread: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        pref = Pref.getInstance(applicationContext)
        topMessage = findViewById(R.id.message)
        catalog = findViewById(R.id.catalog)
        thread = findViewById(R.id.thread)
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
        topMessage.visibility = visibleOrGone(pref.lastCatalogUrl.isEmpty() && pref.lastThreadUrl.isEmpty())
        catalog.visibility = visibleOrGone(pref.lastCatalogUrl.isNotEmpty())
        thread.visibility = visibleOrGone(pref.lastThreadUrl.isNotEmpty())
    }

    // ボタンクリックイベント
    fun onClickLastThread(@Suppress("UNUSED_PARAMETER") view: View) {
        val intent = Intent(applicationContext, HiddenActivity::class.java)
        intent.putExtra(KEY_EXTRA_URL, pref.lastThreadUrl)
        startActivity(intent)
    }

    fun onClickLastCatalog(@Suppress("UNUSED_PARAMETER") view: View) {
        val intent = Intent(applicationContext, HiddenActivity::class.java)
        intent.putExtra(KEY_EXTRA_URL, pref.lastCatalogUrl)
        startActivity(intent)
    }

    fun onClickSettings(@Suppress("UNUSED_PARAMETER") view: View) {
        startActivity(Intent(this, SettingsActivity::class.java))
    }

}
