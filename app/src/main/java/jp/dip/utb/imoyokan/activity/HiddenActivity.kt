package jp.dip.utb.imoyokan.activity

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import jp.dip.utb.imoyokan.util.KEY_EXTRA_URL
import jp.dip.utb.imoyokan.notification.ThreadNotification
import jp.dip.utb.imoyokan.futaba.util.analyseCatalogUrl
import jp.dip.utb.imoyokan.futaba.util.analyseUrl
import jp.dip.utb.imoyokan.notification.CatalogNotification

class HiddenActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val url = intent?.dataString
            ?: intent.getStringExtra(Intent.EXTRA_TEXT)
            ?: intent.getStringExtra(KEY_EXTRA_URL)
        intent.putExtra(KEY_EXTRA_URL, url)
        when {
            url == null -> { /* なにもしない */ }
            analyseCatalogUrl(url) != null -> CatalogNotification(
                this,
                intent
            ).notifyThis()
            analyseUrl(url) != null -> ThreadNotification(
                this,
                intent
            ).notify()
            else -> Toast.makeText(applicationContext, "URLが変！ $url", Toast.LENGTH_LONG).show()
        }
        finish()
    }

}