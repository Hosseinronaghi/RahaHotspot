package ir.raha.hotspot
import android.content.Context
import android.net.TetheringManager
import android.os.Build
class HotspotMonitor(private val context: Context, private val report: (String) -> Unit) {
    private var cleanup: (() -> Unit)? = null
    fun start() {
        if (Build.VERSION.SDK_INT < 36) { report("unknown"); return }
        try { startApi36() } catch (_: Exception) { report("unknown") }
    }
    @android.annotation.TargetApi(36)
    private fun startApi36() {
        val manager = context.getSystemService(TetheringManager::class.java) ?: return
        val callback = object : TetheringManager.TetheringEventCallback {
            override fun onTetheredInterfacesChanged(interfaces: Set<android.net.TetheringInterface>) {
                report(if (interfaces.any { it.type == TetheringManager.TETHERING_WIFI }) "active" else "inactive")
            }
        }
        manager.registerTetheringEventCallback(context.mainExecutor, callback)
        cleanup = { manager.unregisterTetheringEventCallback(callback) }
    }
    fun close() { runCatching { cleanup?.invoke() }; cleanup = null }
}