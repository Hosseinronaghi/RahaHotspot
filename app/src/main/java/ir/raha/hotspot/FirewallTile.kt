package ir.raha.hotspot
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
class FirewallTile : TileService() {
    override fun onStartListening() {
        super.onStartListening()
        qsTile?.apply {
            state = if (FirewallService.state == "active") Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
            label = "Raha Hotspot"
            updateTile()
        }
    }
    override fun onClick() {
        super.onClick()
        if (FirewallService.state == "active") {
            stopService(Intent(this, FirewallService::class.java))
        } else {
            val intent = Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            if (Build.VERSION.SDK_INT >= 34) startActivityAndCollapse(PendingIntent.getActivity(this, 3, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT))
            else @Suppress("DEPRECATION") startActivityAndCollapse(intent)
        }
    }
}