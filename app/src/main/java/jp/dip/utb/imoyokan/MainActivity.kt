package jp.dip.utb.imoyokan

import android.app.AlertDialog
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import android.widget.Switch
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity


class MainActivity : AppCompatActivity(), SharedPreferences.OnSharedPreferenceChangeListener {

    private lateinit var pref: Pref
    private lateinit var topMessage: TextView
    private lateinit var catalog: TextView
    private lateinit var thread: TextView
    private lateinit var catalogCols: TextView
    private lateinit var catalogRows: TextView
    private lateinit var shortKitaa: Switch

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        pref = Pref.getInstance(applicationContext)
        topMessage = findViewById(R.id.message)
        catalog = findViewById(R.id.catalog)
        thread = findViewById(R.id.thread)
        catalogCols = findViewById(R.id.catalog_cols)
        catalogRows = findViewById(R.id.catalog_rows)
        shortKitaa = findViewById(R.id.short_kitaa)
        shortKitaa.setTextColor(catalogCols.textColors.defaultColor)
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

    private fun refresh() {
        topMessage.visibility = toggleVisible(pref.lastCatalogUrl.isEmpty() && pref.lastThreadUrl.isEmpty())
        catalog.visibility = toggleVisible(pref.lastCatalogUrl.isNotEmpty())
        thread.visibility = toggleVisible(pref.lastThreadUrl.isNotEmpty())
        catalogCols.text = pref.catalog.cols.toString()
        catalogRows.text = pref.catalog.rows.toString()
        shortKitaa.isChecked = pref.thread.shortKitaa
    }

    private fun toggleVisible(b: Boolean): Int {
        return if (b) View.VISIBLE else View.GONE
    }

    // イベント

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

    fun onClickSwitch(@Suppress("UNUSED_PARAMETER") view: View) {
        pref.thread.shortKitaa = shortKitaa.isChecked
    }

}

