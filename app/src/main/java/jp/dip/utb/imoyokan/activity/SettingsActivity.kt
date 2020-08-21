package jp.dip.utb.imoyokan.activity

import android.content.SharedPreferences
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import jp.dip.utb.imoyokan.R
import jp.dip.utb.imoyokan.model.Pref


class SettingsActivity : AppCompatActivity(), SharedPreferences.OnSharedPreferenceChangeListener {

    lateinit var pref: Pref

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.settings_activity)
        pref = Pref.getInstance(this)
        val fragment =
            SettingsFragment(
                pref.debugMode
            )
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.settings, fragment)
            .commit()
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    class SettingsFragment(val debugMode: Boolean): PreferenceFragmentCompat() {

        private var versionClickCount = 3

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey)

            // デバッグモード表示
            if (debugMode) {
                findPreference<Preference>("debug_mode")?.isVisible = true
            }
            findPreference<Preference>("version")?.setOnPreferenceClickListener {
                if (versionClickCount <= 1) {
                    findPreference<Preference>("debug_mode")?.isVisible = true
                } else {
                    versionClickCount --
                }
                true
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            // Respond to the action bar's Up/Home button
            android.R.id.home -> {
                super.onBackPressed()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
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
    }

}
