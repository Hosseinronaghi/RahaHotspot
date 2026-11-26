package ir.raha.hotspot
import android.app.*
import android.content.Intent
import android.net.VpnService
import android.os.*
import java.util.concurrent.atomic.AtomicLong
class FirewallService : VpnService() {
    companion object {
        const val START = "ir.raha.hotspot.START"
        const val STOP = "ir.raha.hotspot.STOP"
        @Volatile var state = "idle"; private set
        val packets = AtomicLong(); val bytes = AtomicLong()
    }
    private var tun: ParcelFileDescriptor? = null
    private var worker: Thread? = null
    private var generation = 0
    private var started = 0L
    @Volatile private var deadline = 0L
    private var mode = Mode.ALL
    private val timeout = Runnable { shutdown() }
    private val main = Handler(Looper.getMainLooper())
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == STOP) { shutdown(); return START_NOT_STICKY }
        if (intent?.action != START) { shutdown(); return START_NOT_STICKY }
        if (tun != null) return START_NOT_STICKY
        state = "starting"
        try {
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(NotificationChannel("firewall", getString(R.string.channel), NotificationManager.IMPORTANCE_LOW))
            val stop = PendingIntent.getService(this, 1, Intent(this, FirewallService::class.java).setAction(STOP), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
            val open = PendingIntent.getActivity(this, 2, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
            startForeground(10, Notification.Builder(this, "firewall").setSmallIcon(android.R.drawable.ic_lock_lock).setContentTitle("Raha Hotspot").setContentText(getString(R.string.warning)).setContentIntent(open).addAction(Notification.Action.Builder(null, getString(R.string.stop), stop).build()).setOngoing(true).build())
            mode = Mode.valueOf(intent.getStringExtra("mode") ?: "ALL")
            val policy = Policy(mode, intent.getStringArrayListExtra("packages")?.toSet() ?: emptySet())
            val builder = Builder().setSession("Raha Hotspot").setMtu(1500).addAddress("10.77.0.1",32).addAddress("fd77:7261:6861::1",128).addRoute("0.0.0.0",0).addRoute("::",0).addDnsServer("10.77.0.2").addDnsServer("fd77:7261:6861::2").setBlocking(false)
            when (policy.mode) {
                Mode.ALL -> Unit
                Mode.ALLOW -> policy.packages.forEach { builder.addDisallowedApplication(it) }
                Mode.BLOCK -> policy.packages.forEach { builder.addAllowedApplication(it) }
            }
            val descriptor = builder.establish() ?: error("VPN unavailable")
            tun = descriptor
            started = SystemClock.elapsedRealtime()
            packets.set(0); bytes.set(0)
            val token = ++generation
            state = "active"
            val minutes = intent.getIntExtra("minutes", 0).coerceIn(0, 1440)
            deadline = if (minutes > 0) SystemClock.elapsedRealtime() + minutes * 60_000L else 0L
            if (minutes > 0) main.postDelayed(timeout, minutes * 60_000L)
            refreshTile()
            worker = Thread {
                try {
                    val buffer = ByteArray(32768)
                    val polls = arrayOf(android.system.StructPollfd().apply { fd = descriptor.fileDescriptor; events = android.system.OsConstants.POLLIN.toShort() })
                    while (!Thread.currentThread().isInterrupted) {
                        polls[0].revents = 0
                        if (android.system.Os.poll(polls, 10000) == 0) continue
                        if ((polls[0].revents.toInt() and android.system.OsConstants.POLLIN) == 0) error("TUN unavailable")
                        val count = try { android.system.Os.read(descriptor.fileDescriptor, buffer, 0, buffer.size) } catch (e: android.system.ErrnoException) { if (e.errno == android.system.OsConstants.EAGAIN || e.errno == android.system.OsConstants.EINTR) continue else throw e }
                        if (count <= 0) error("TUN closed")
                        if (deadline != 0L && SystemClock.elapsedRealtime() >= deadline) { main.post { if (token == generation) shutdown() }; break }
                        packets.incrementAndGet(); bytes.addAndGet(count.toLong())
                    }
                } catch (_: Exception) { main.post { if (token == generation) shutdown("error") } }
            }.apply { name = "raha-tun-drop"; start() }
        } catch (_: Exception) { shutdown("error") }
        return START_NOT_STICKY
    }
    private fun release() { if (started != 0L) { val prefs = getSharedPreferences("history", MODE_PRIVATE); val entry = "${System.currentTimeMillis()}|${SystemClock.elapsedRealtime()-started}|${packets.get()}|${bytes.get()}|$mode"; prefs.edit().putString("sessions", (listOf(entry) + (prefs.getString("sessions", "") ?: "").lineSequence().filter { it.isNotBlank() }.toList()).take(50).joinToString("\n")).apply(); started = 0L }; main.removeCallbacks(timeout); generation++; worker?.interrupt(); worker = null; try { tun?.close() } catch (_: Exception) {}; tun = null }
    private fun shutdown(result: String = "idle") { release(); state = result; refreshTile(); stopForeground(STOP_FOREGROUND_REMOVE); stopSelf() }
    private fun refreshTile() { android.service.quicksettings.TileService.requestListeningState(this, android.content.ComponentName(this, FirewallTile::class.java)) }
    override fun onRevoke() { main.post { shutdown("revoked") } }
    override fun onDestroy() { release(); if (state == "active" || state == "starting") state = "idle"; refreshTile(); super.onDestroy() }
}