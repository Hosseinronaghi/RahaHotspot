package ir.raha.hotspot
import android.os.Bundle
import android.content.Intent
import android.net.VpnService
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
class MainActivity : ComponentActivity() {
    private var pending: Intent? = null
    private val consent = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) pending?.let { startForegroundService(it) }; pending = null
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val fa = resources.configuration.locales[0].language == "fa"
        fun tr(en: String, persian: String) = if(fa) persian else en
        val apps = packageManager.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER),0).map { it.activityInfo.packageName to it.loadLabel(packageManager).toString() }.distinctBy { it.first }.sortedBy { it.second.lowercase() }
        setContent { val family = androidx.compose.ui.text.font.FontFamily(androidx.compose.ui.text.font.Font(R.font.vazirmatn_regular), androidx.compose.ui.text.font.Font(R.font.vazirmatn_bold, androidx.compose.ui.text.font.FontWeight.Bold))
val defaults = Typography()
val typography = defaults.copy(displayLarge=defaults.displayLarge.copy(fontFamily=family),displayMedium=defaults.displayMedium.copy(fontFamily=family),displaySmall=defaults.displaySmall.copy(fontFamily=family),headlineLarge=defaults.headlineLarge.copy(fontFamily=family),headlineMedium=defaults.headlineMedium.copy(fontFamily=family),headlineSmall=defaults.headlineSmall.copy(fontFamily=family),titleLarge=defaults.titleLarge.copy(fontFamily=family),titleMedium=defaults.titleMedium.copy(fontFamily=family),titleSmall=defaults.titleSmall.copy(fontFamily=family),bodyLarge=defaults.bodyLarge.copy(fontFamily=family),bodyMedium=defaults.bodyMedium.copy(fontFamily=family),bodySmall=defaults.bodySmall.copy(fontFamily=family),labelLarge=defaults.labelLarge.copy(fontFamily=family),labelMedium=defaults.labelMedium.copy(fontFamily=family),labelSmall=defaults.labelSmall.copy(fontFamily=family))
MaterialTheme(typography=typography) { Surface {
            var mode by remember { mutableStateOf(Mode.ALL) }
            var selected by remember { mutableStateOf(setOf<String>()) }
            var minutes by remember { mutableIntStateOf(0) }
            var state by remember { mutableStateOf(FirewallService.state) }
            var bytes by remember { mutableLongStateOf(0) }
            var history by remember { mutableStateOf("") }
            var mobile by remember { mutableStateOf(MobileCounters.read()) }
            var hotspot by remember { mutableStateOf("unknown") }
            DisposableEffect(Unit) { val monitor = HotspotMonitor(this@MainActivity) { hotspot = it }; monitor.start(); onDispose { monitor.close() } }
            var query by remember { mutableStateOf("") }
            LaunchedEffect(Unit) { while(true) { state = FirewallService.state; bytes = FirewallService.bytes.get(); history = getSharedPreferences("history", MODE_PRIVATE).getString("sessions", "") ?: ""; mobile = MobileCounters.read(); delay(3000) } }
            LazyColumn(Modifier.fillMaxSize().safeDrawingPadding().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                item { Text("Raha Hotspot 1.0 RC4", style = MaterialTheme.typography.headlineSmall); Text(getString(R.string.warning)); Text("$state · $bytes " + tr("discarded bytes", "بایت حذف‌شده")); Text(tr("Hotspot: ", "هات‌اسپات: ") + when(hotspot) { "active" -> tr("active", "فعال"); "inactive" -> tr("inactive", "غیرفعال"); else -> tr("unknown", "نامشخص") }); Text(tr("Mobile interface totals since boot (not per client): ", "مجموع رابط موبایل از زمان روشن شدن (نه به تفکیک دستگاه): ") + (mobile?.let { "RX ${it.first} B / TX ${it.second} B" } ?: tr("Unavailable", "در دسترس نیست"))); Text(tr("VPN failure can restore internet access.", "خرابی VPN ممکن است اینترنت گوشی را دوباره برقرار کند.")) }
                item { Button(onClick = { startActivity(Intent(Settings.ACTION_WIRELESS_SETTINGS)) }) { Text(getString(R.string.settings)) } }
                item { Text(tr("Firewall mode", "حالت فایروال")); Mode.entries.forEach { m -> Row { RadioButton(selected = mode == m, onClick = { mode = m; selected = emptySet() }, enabled = state != "active"); Text(when(m) { Mode.ALL -> tr("Block all", "مسدودسازی همه"); Mode.ALLOW -> tr("Allow selected", "فهرست سفید"); Mode.BLOCK -> tr("Block selected", "فهرست سیاه") }) } } }
                item { Text(tr("Duration (minutes)", "مدت (دقیقه)")); Row { listOf(0,15,30,60,120,480).forEach { n -> FilterChip(selected = minutes == n, onClick = { minutes = n }, label = { Text(if(n==0) "∞" else "$n") }) } } }
                item { Button(enabled = state != "active" && state != "starting" && (mode != Mode.BLOCK || selected.isNotEmpty()), onClick = { pending = Intent(this@MainActivity, FirewallService::class.java).setAction(FirewallService.START).putExtra("minutes",minutes).putExtra("mode",mode.name).putStringArrayListExtra("packages",ArrayList(selected)); val request = VpnService.prepare(this@MainActivity); if(request != null) consent.launch(request) else { pending?.let { startForegroundService(it) }; pending = null } }) { Text(getString(R.string.start)) }; OutlinedButton(onClick = { stopService(Intent(this@MainActivity,FirewallService::class.java)) }) { Text(getString(R.string.stop)) } }
                if(mode != Mode.ALL) { item { OutlinedTextField(query,{query=it},label={Text(tr("Search apps", "جستجوی برنامه‌ها"))}) }; items(apps.filter { it.first.contains(query,true) || it.second.contains(query,true) }, key={it.first}) { (pkg,label) -> Row { Checkbox(pkg in selected, { checked -> selected = if(checked) selected + pkg else selected - pkg }, enabled = state != "active"); Column { Text(label); Text(pkg,style=MaterialTheme.typography.bodySmall) } } } }
                item { Text(tr("Recent sessions (up to 50)", "نشست‌های اخیر (تا ۵۰ مورد)")); OutlinedButton(onClick={getSharedPreferences("history",MODE_PRIVATE).edit().clear().apply(); history=""}) { Text(tr("Clear history", "پاک کردن تاریخچه")) } }
                items(history.lines().filter { it.isNotBlank() }) { line -> val fields=line.split('|'); if(fields.size==5) Text("${java.text.DateFormat.getDateTimeInstance().format(java.util.Date(fields[0].toLongOrNull() ?: 0))} · ${fields[1].toLongOrNull()?.div(1000)} s · ${fields[2]} packets · ${fields[3]} B · ${fields[4]}") }
            }
        } } }
    }
}