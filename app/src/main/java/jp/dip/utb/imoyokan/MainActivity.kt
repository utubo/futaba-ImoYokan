package jp.dip.utb.imoyokan

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import jp.dip.utb.imoyokan.futaba.getCatalogUrl
import android.content.SharedPreferences
import androidx.core.app.ComponentActivity.ExtraData
import androidx.core.content.ContextCompat.getSystemService
import android.icu.lang.UCharacter.GraphemeClusterBreak.T
import android.util.Log


class MainActivity : AppCompatActivity(), SharedPreferences.OnSharedPreferenceChangeListener {

    private lateinit var pref: Pref

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        pref = Pref(applicationContext)
        refresh()
    }

    override fun onResume() {
        super.onResume()
        pref.pref.registerOnSharedPreferenceChangeListener(this)
    }

    override fun onPause() {
        super.onPause()
        pref.pref.unregisterOnSharedPreferenceChangeListener(this)
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String) {
        pref.cache.remove(key)
        refresh()
    }

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

    private fun refresh() {
        findViewById<TextView>(R.id.message).visibility =
            if (pref.lastCatalogUrl.isEmpty() && pref.lastThreadUrl.isEmpty()) {
                View.VISIBLE
            } else {
                View.GONE
            }

        findViewById<TextView>(R.id.catalog).visibility =
            if (pref.lastCatalogUrl.isNotEmpty()) {
                View.VISIBLE
            } else {
                View.GONE
            }

        findViewById<TextView>(R.id.thread).visibility =
            if (pref.lastThreadUrl.isNotEmpty()) {
                View.VISIBLE
            } else {
                View.GONE
            }

        findViewById<TextView>(R.id.catalog_cols).text = pref.catalog.cols.toString()
        findViewById<TextView>(R.id.catalog_rows).text = pref.catalog.rows.toString()
    }

    fun onClickCatalogCols(@Suppress("UNUSED_PARAMETER") view: View) {
        val items = arrayOf("3", "4", "5", "6", "7", "8", "9")
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.catalog_cols))
            .setItems(items) { _, which ->
                pref.catalog.cols = items[which].toInt()
                pref.apply()
            }
            .show()
    }

    fun onClickCatalogRows(@Suppress("UNUSED_PARAMETER") view: View) {
        val items = arrayOf("1", "2", "3", "4", "5")
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.catalog_rows))
            .setItems(items) { _, which ->
                pref.catalog.rows = items[which].toInt()
                pref.apply()
            }
            .show()
    }

}

