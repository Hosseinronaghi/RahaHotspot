package ir.raha.hotspot
import android.net.TrafficStats
object MobileCounters {
    fun read(): Pair<Long, Long>? {
        val rx = TrafficStats.getMobileRxBytes()
        val tx = TrafficStats.getMobileTxBytes()
        return if (rx < 0 || tx < 0) null else rx to tx
    }
}