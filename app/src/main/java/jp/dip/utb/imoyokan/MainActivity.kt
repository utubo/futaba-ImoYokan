package jp.dip.utb.imoyokan

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import jp.dip.utb.imoyokan.futaba.analyseCatalogUrl
import jp.dip.utb.imoyokan.futaba.analyseUrl

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val url = intent?.dataString ?: intent.getStringExtra(Intent.EXTRA_TEXT)
        intent.putExtra(Intent.EXTRA_TEXT, url)
        if (url != null) {
            val err = when {
                    analyseCatalogUrl(url) != null -> { CatalogNotification.notify(CatalogNotification(this, intent)); null }
                    analyseUrl(url) != null -> { ThreadNotification(this, intent).notify(); null }
                    else -> "URLが変！ $url"
                }
            Toast.makeText(applicationContext, err ?: "通知領域に表示します", Toast.LENGTH_LONG).show()
        }
        finish()
    }

}

