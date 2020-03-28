package jp.dip.utb.imoyokan

import android.content.Intent
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService

class QSTileService : TileService() {

    override fun onTileAdded() {
        super.onTileAdded()
        qsTile.state = Tile.STATE_ACTIVE
        qsTile.updateTile()
    }

    override fun onClick() {
        val intent = Intent(applicationContext, NotificationReceiver::class.java)
        intent.putExtra(KEY_EXTRA_URL, Pref.getInstance(applicationContext).lastUrl)
        sendBroadcast(intent)
    }
}