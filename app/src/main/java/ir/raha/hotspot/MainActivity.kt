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
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.border
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
class MainActivity : ComponentActivity() {
 private var pending: Intent? = null
 private val consent = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
 if(result.resultCode == RESULT_OK) pending?.let { startForegroundService(it) }; pending=null
 }
 override fun onCreate(savedInstanceState: Bundle?) {
 super.onCreate(savedInstanceState)
 val prefs=getSharedPreferences("appearance", MODE_PRIVATE)
 val apps=packageManager.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER),0).map { it.activityInfo.packageName to it.loadLabel(packageManager).toString() }.distinctBy { it.first }.sortedBy { it.second.lowercase() }
 setContent {
 var language by remember { mutableStateOf(prefs.getString("language","system") ?: "system") }
 var theme by remember { mutableStateOf(prefs.getString("theme","system") ?: "system") }
 val fa=language=="fa" || (language=="system" && resources.configuration.locales[0].language=="fa")
 fun tr(en:String, persian:String)=if(fa) persian else en
 val dark=theme=="dark" || (theme=="system" && isSystemInDarkTheme())
 val family = androidx.compose.ui.text.font.FontFamily(androidx.compose.ui.text.font.Font(R.font.vazirmatn_regular), androidx.compose.ui.text.font.Font(R.font.vazirmatn_bold, androidx.compose.ui.text.font.FontWeight.Bold))
val defaults = Typography()
val typography = defaults.copy(displayLarge=defaults.displayLarge.copy(fontFamily=family),displayMedium=defaults.displayMedium.copy(fontFamily=family),displaySmall=defaults.displaySmall.copy(fontFamily=family),headlineLarge=defaults.headlineLarge.copy(fontFamily=family),headlineMedium=defaults.headlineMedium.copy(fontFamily=family),headlineSmall=defaults.headlineSmall.copy(fontFamily=family),titleLarge=defaults.titleLarge.copy(fontFamily=family),titleMedium=defaults.titleMedium.copy(fontFamily=family),titleSmall=defaults.titleSmall.copy(fontFamily=family),bodyLarge=defaults.bodyLarge.copy(fontFamily=family),bodyMedium=defaults.bodyMedium.copy(fontFamily=family),bodySmall=defaults.bodySmall.copy(fontFamily=family),labelLarge=defaults.labelLarge.copy(fontFamily=family),labelMedium=defaults.labelMedium.copy(fontFamily=family),labelSmall=defaults.labelSmall.copy(fontFamily=family))

 val colors=if(dark) darkColorScheme(primary=Color(0xFF67D9D0),secondary=Color(0xFF9CBFE8)) else lightColorScheme(primary=Color(0xFF006B68),secondary=Color(0xFF234D78),background=Color(0xFFF3F7FA))
 CompositionLocalProvider(LocalLayoutDirection provides if(fa) LayoutDirection.Rtl else LayoutDirection.Ltr) {
 MaterialTheme(colorScheme=colors,typography=typography) {
 var tab by remember { mutableIntStateOf(0) }
 var mode by remember { mutableStateOf(Mode.ALL) }
 var selected by remember { mutableStateOf(setOf<String>()) }
 var minutes by remember { mutableIntStateOf(0) }
 var state by remember { mutableStateOf(FirewallService.state) }
 var bytes by remember { mutableLongStateOf(0) }
 var history by remember { mutableStateOf("") }
 var mobile by remember { mutableStateOf(MobileCounters.read()) }
 var hotspot by remember { mutableStateOf("unknown") }
 var query by remember { mutableStateOf("") }
 val active=state=="active"
 val busy=active || state=="starting"
 fun modeName(m:Mode)=when(m){Mode.ALL->tr("Block all","مسدودسازی همه");Mode.ALLOW->tr("Allow selected","فهرست سفید");Mode.BLOCK->tr("Block selected","فهرست سیاه")}
 DisposableEffect(Unit){val monitor=HotspotMonitor(this@MainActivity){hotspot=it};monitor.start();onDispose{monitor.close()}}
 LaunchedEffect(Unit){while(true){state=FirewallService.state;bytes=FirewallService.bytes.get();history=getSharedPreferences("history",MODE_PRIVATE).getString("sessions","") ?: "";mobile=MobileCounters.read();delay(3000)}}
 Scaffold(bottomBar={NavigationBar { listOf(tr("Home","خانه"),tr("Apps","برنامه‌ها"),tr("Stats","آمار"),tr("Settings","تنظیمات")).forEachIndexed { i,label -> NavigationBarItem(selected=tab==i,onClick={tab=i},icon={Text(listOf("⌂","▦","▥","⚙")[i],style=MaterialTheme.typography.titleLarge)},label={Text(label)}) } }}) { padding ->
 LazyColumn(Modifier.fillMaxSize().padding(padding).padding(horizontal=20.dp),verticalArrangement=Arrangement.spacedBy(12.dp),contentPadding=PaddingValues(vertical=16.dp)){
 item { Text(tr("Raha Hotspot","رها هات‌اسپات"),style=MaterialTheme.typography.headlineMedium) }
 when(tab){
 0->{
 item { Box(Modifier.fillMaxWidth(),contentAlignment=Alignment.Center){Column(Modifier.size(210.dp).border(5.dp,if(hotspot=="active") Color(0xFF16A36A) else MaterialTheme.colorScheme.outline,CircleShape),verticalArrangement=Arrangement.Center,horizontalAlignment=Alignment.CenterHorizontally){Text("◉",style=MaterialTheme.typography.displayMedium);Text(when(hotspot){"active"->tr("Hotspot active","هات‌اسپات فعال");"inactive"->tr("Hotspot inactive","هات‌اسپات غیرفعال");else->tr("Hotspot unknown","وضعیت نامشخص")})}} }
 item { Card(Modifier.fillMaxWidth()){Column(Modifier.padding(16.dp)){Row(verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text(tr("Internet control","کنترل اینترنت"),style=MaterialTheme.typography.titleMedium);Text(when(state){"active"->tr("Blocking active","محافظت فعال");"starting"->tr("Starting","در حال اتصال");"error","revoked"->tr("VPN stopped or failed","خطا یا توقف VPN");else->tr("Inactive","غیرفعال")})};Switch(checked=active,enabled=state!="starting" && (active || mode!=Mode.BLOCK || selected.isNotEmpty()),onCheckedChange={on->if(!on){stopService(Intent(this@MainActivity,FirewallService::class.java))}else{pending=Intent(this@MainActivity,FirewallService::class.java).setAction(FirewallService.START).putExtra("minutes",minutes).putExtra("mode",mode.name).putStringArrayListExtra("packages",ArrayList(selected));val request=VpnService.prepare(this@MainActivity);if(request!=null)consent.launch(request)else{pending?.let{startForegroundService(it)};pending=null}}})};Text(modeName(mode));Text(tr("Dropped bytes: ","بایت مسدودشده: ")+bytes)}} }
 item { OutlinedButton(onClick={startActivity(Intent(Settings.ACTION_WIRELESS_SETTINGS))},modifier=Modifier.fillMaxWidth()){Text(tr("Wireless settings","تنظیمات شبکه"))} }
 item { Text(tr("Experimental: hotspot compatibility is unverified. Internet may return if VPN stops. Another VPN may disconnect.","آزمایشی: سازگاری هات‌اسپات تأیید نشده است. با توقف VPN اینترنت ممکن است برقرار شود و VPN دیگر قطع شود."),style=MaterialTheme.typography.bodySmall) }
 }
 1->{item{Text(tr("Firewall mode","حالت فایروال"),style=MaterialTheme.typography.titleLarge);Mode.entries.forEach{m->Row(verticalAlignment=Alignment.CenterVertically){RadioButton(mode==m,{mode=m;selected=emptySet()},enabled=!busy);Text(modeName(m))}}}
 if(mode!=Mode.ALL){item{OutlinedTextField(query,{query=it},modifier=Modifier.fillMaxWidth(),label={Text(tr("Search apps","جستجوی برنامه‌ها"))})};items(apps.filter{it.first.contains(query,true)||it.second.contains(query,true)},key={it.first}){(pkg,label)->Row(verticalAlignment=Alignment.CenterVertically){Checkbox(pkg in selected,{checked->selected=if(checked)selected+pkg else selected-pkg},enabled=!busy);Column{Text(label);Text(pkg,style=MaterialTheme.typography.bodySmall)}}}}}
 2->{item{Card(Modifier.fillMaxWidth()){Column(Modifier.padding(16.dp)){Text(tr("Mobile totals since boot (not per client)","مجموع اینترنت موبایل از زمان روشن شدن (نه به تفکیک دستگاه)"));Text(mobile?.let{"RX ${it.first} B / TX ${it.second} B"}?:tr("Unavailable","در دسترس نیست"));Text(tr("Dropped bytes: ","بایت مسدودشده: ")+bytes)}}}
 item{Text(tr("Recent sessions","نشست‌های اخیر"));OutlinedButton(onClick={getSharedPreferences("history",MODE_PRIVATE).edit().clear().apply();history=""}){Text(tr("Clear history","پاک کردن تاریخچه"))}}
 items(history.lines().filter{it.isNotBlank()}){line->val f=line.split('|');if(f.size==5)Card(Modifier.fillMaxWidth()){Column(Modifier.padding(12.dp)){Text(java.text.DateFormat.getDateTimeInstance().format(java.util.Date(f[0].toLongOrNull()?:0)));Text("${f[1].toLongOrNull()?.div(1000)} "+tr("seconds","ثانیه")+" · ${f[3]} B");Text(runCatching{modeName(Mode.valueOf(f[4]))}.getOrDefault(f[4]))}}}}
 3->{item{Text(tr("Language","زبان"));listOf("system" to tr("Device language","زبان دستگاه"),"fa" to "فارسی","en" to "English").forEach{(v,l)->Row(verticalAlignment=Alignment.CenterVertically){RadioButton(language==v,{language=v;prefs.edit().putString("language",v).apply()});Text(l)}}}
 item{Text(tr("Appearance","ظاهر"));listOf("system" to tr("System","سیستم"),"light" to tr("Light","روشن"),"dark" to tr("Dark","تاریک")).forEach{(v,l)->Row(verticalAlignment=Alignment.CenterVertically){RadioButton(theme==v,{theme=v;prefs.edit().putString("theme",v).apply()});Text(l)}}}
 item{Text(tr("Auto stop (minutes)","توقف خودکار (دقیقه)"));listOf(0,15,30,60,120,480).chunked(3).forEach{group->Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){group.forEach{n->FilterChip(minutes==n,{minutes=n},label={Text(if(n==0)tr("Unlimited","نامحدود") else n.toString())},enabled=!busy)}}}}
 item{Text("1.0.0 RC5")}}
 }
 }
 }
 }
 }
 }
 }
}
