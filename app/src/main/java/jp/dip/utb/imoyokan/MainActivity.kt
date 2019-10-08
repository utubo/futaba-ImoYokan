package jp.dip.utb.imoyokan

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val data = intent?.dataString ?: intent.getStringExtra(Intent.EXTRA_TEXT)
        intent.putExtra(KEY_EXTRA_URL, data)
        if (data != null) {
            val msg = ThreadNotification().showThread(this, intent)
            if (msg.isNotEmpty()) {
                Toast.makeText(applicationContext, msg, Toast.LENGTH_LONG).show()
            }
        }
        finish()
    }

}

