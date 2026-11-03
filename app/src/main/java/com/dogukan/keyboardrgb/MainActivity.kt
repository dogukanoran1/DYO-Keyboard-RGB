package com.dogukan.keyboardrgb

import android.Manifest
import android.bluetooth.BluetoothDevice
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Build
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/** Stage 1: a diagnostic screen, not a simulated lighting-control product. */
class MainActivity : ComponentActivity() {
    private lateinit var manager: BluetoothDeviceManager
    private lateinit var analyzer: KeyboardHardwareAnalyzer
    private lateinit var observer: LightingObserver
    private var observing by mutableStateOf(false)
    private val prefs by lazy { getSharedPreferences("analysis", MODE_PRIVATE) }
    private var devices by mutableStateOf(emptyList<BluetoothDevice>())
    private var selected by mutableStateOf<String?>(null)
    private var report by mutableStateOf("")
    private var busy by mutableStateOf(false)
    private var status by mutableStateOf("Yakındaki cihazlar iznini verip eşleştirilmiş klavyeni seç.")
    private val permissions = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { refresh() }
    private val notificationPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { startSleepTrial() }
    private val export = registerForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if(uri != null) try {
            val output = contentResolver.openOutputStream(uri) ?: error("Dosya açılamadı")
            output.bufferedWriter().use { it.write(report) }; status = "Teknik rapor kaydedildi."
        } catch(e: Exception) { status = "Kaydedilemedi: ${e.message}" }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        manager = BluetoothDeviceManager(this); analyzer = KeyboardHardwareAnalyzer(this, manager); observer = LightingObserver(this)
        selected = prefs.getString("selected", null); report = prefs.getString("report", "") ?: ""
        SleepTrialService.restoreReport(this)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme(background = Color.Black, surface = Color(0xFF111111), primary = Color(0xFF9FCFFF))) {
                Surface(modifier = Modifier.fillMaxSize(), color = Color.Black) {
                    BoxWithConstraints(Modifier.fillMaxSize().systemBarsPadding()) {
                        val wide = maxWidth >= 840.dp
                        if(wide) Row(Modifier.fillMaxSize().padding(24.dp), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                            Column(Modifier.width(340.dp).fillMaxHeight().verticalScroll(rememberScrollState())) { Controls() }
                            Report(Modifier.weight(1f))
                        } else LazyColumn(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            item { Controls() }
                            item { ReportText() }
                        }
                    }
                }
            }
        }
        refresh()
    }
    override fun onResume() { super.onResume(); if(::manager.isInitialized && !busy && !SleepTrialService.running) refresh() }
    private fun allowed() = checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED &&
        checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED
    @android.annotation.SuppressLint("MissingPermission")
    private fun refresh() {
        if(!allowed()) { devices = emptyList(); status = "Yakındaki cihazlar izni gerekli. İzin reddedildiyse uygulama ayarlarından açabilirsin."; return }
        try {
            devices = manager.pairedDevices()
            status = when { manager.adapter == null -> "Bu cihazda Bluetooth adaptörü yok."
                manager.adapter?.isEnabled != true -> "Bluetooth kapalı. Sistem ayarlarından aç."
                devices.isEmpty() -> "Eşleştirilmiş cihaz bulunamadı. Önce Android Bluetooth ayarlarından klavyeyi eşleştir."
                else -> "${devices.size} eşleştirilmiş cihaz. Eşleştirme durumu, canlı bağlantı durumu değildir." }
        } catch(e: Exception) { devices = emptyList(); status = "Cihazlar okunamadı: ${e.message}" }
    }
    @android.annotation.SuppressLint("MissingPermission")
    private fun analyze() {
        if(!allowed()) { refresh(); return }
        val device = devices.find { it.address == selected } ?: return
        busy = true
        analyzer.analyze(device) { json, running ->
            report = json; busy = running
            if(!running) { prefs.edit().putString("report", json).apply(); status = "Analiz sona erdi. LED kontrolü henüz doğrulanmadı." }
        }
    }
    @android.annotation.SuppressLint("MissingPermission")
    private fun observe() {
        if (!allowed()) { refresh(); return }
        val device = devices.find { it.address == selected } ?: return
        busy = true; observing = true
        observer.start(device) { json, running ->
            report = json; busy = running
            if (!running) { observing = false; prefs.edit().putString("report", json).apply(); status = "Gözlem sona erdi. Raporu kaydedip gönder." }
            else status = try { org.json.JSONObject(json).optString("status") } catch (_: Exception) { "Gözlem sürüyor" }
        }
    }
    @android.annotation.SuppressLint("MissingPermission")
    private fun startSleepTrial() {
        if (!allowed()) { refresh(); return }
        if (busy || SleepTrialService.running) return
        val address = devices.find { it.address == selected }?.address ?: return
        try {
            startForegroundService(Intent(this, SleepTrialService::class.java).putExtra(SleepTrialService.EXTRA_ADDRESS, address))
            status = "Uyku deneyi başlatılıyor. Deney uygulama arka plandayken de sürer."
        } catch (e: Exception) { status = "Uyku deneyi başlatılamadı: ${e.message}" }
    }
    @Composable
    private fun Controls() {
        val sleepRunning = SleepTrialService.running
        val idle = !busy && !sleepRunning
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("DYO Keyboard RGB", style = MaterialTheme.typography.headlineSmall)
            Text("Donanım Analizi · Aşama 1", color = MaterialTheme.colorScheme.primary)
            Text("LED kontrolü: BELİRSİZ", style = MaterialTheme.typography.titleMedium)
            Text("Rainbow Wave ve renk kontrolü, donanım protokolü doğrulanana kadar kapalı.")
            Text(status, style = MaterialTheme.typography.bodySmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(enabled = idle, onClick = {
                    if(allowed()) refresh() else permissions.launch(arrayOf(Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN))
                }) { Text("İzin / Yenile") }
                TextButton(onClick = { startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS)) }) { Text("Bluetooth") }
            }
            devices.forEach { device ->
                @Suppress("MissingPermission")
                val name = try { device.name ?: "Adsız cihaz" } catch(_: SecurityException) { "İzin gerekli" }
                OutlinedButton(modifier = Modifier.fillMaxWidth(), enabled = idle, onClick = {
                    selected = device.address; prefs.edit().putString("selected", selected).apply()
                    // Old reports remain explicitly labelled by their own address and timestamp.
                }) {
                    Column(Modifier.fillMaxWidth()) {
                        Text((if(selected == device.address) "✓ " else "") + name)
                        Text(device.address, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            Button(modifier = Modifier.fillMaxWidth(), enabled = idle && devices.any { it.address == selected }, onClick = { analyze() }) { Text("Donanımı incele") }
            OutlinedButton(modifier = Modifier.fillMaxWidth(), enabled = idle && devices.any { it.address == selected }, onClick = { observe() }) { Text("Işık tuşlarını gözle") }
            Text("Gözlem başlayınca 45 saniye içinde klavyenin ışık renk ve parlaklık tuşlarına bas. Normal yazı yazma. Bildirim gelmeyebilir.", style = MaterialTheme.typography.bodySmall)
            if(busy) {
                LinearProgressIndicator(Modifier.fillMaxWidth())
                TextButton(onClick = { if(observing) observer.cancel() else analyzer.cancel(); busy = false; status = "İşlem durduruldu; rapor kısmi olabilir." }) { Text("Analizi durdur") }
            }
            HorizontalDivider()
            Text("Uyku geciktirme deneyi", style = MaterialTheme.typography.titleMedium)
            Text("12 dakika boyunca 15 saniyede bir klavyeden üretici bilgisi okunur. Bunun 30 saniyelik ışık ve 10 dakikalık uyku süresine etkisi henüz bilinmiyor. Kalıcı ayar değiştirilmez.", style = MaterialTheme.typography.bodySmall)
            if (sleepRunning) {
                Text("Deney sürüyor. İlk 11 dakika klavyeye dokunma; ışığın açık kalıp kalmadığını gözle. Sonra bir tuşa basıp uyanma gecikmesini kontrol et.", style = MaterialTheme.typography.bodySmall)
                OutlinedButton(onClick = { stopService(Intent(this@MainActivity, SleepTrialService::class.java)) }) { Text("Uyku deneyini durdur") }
            } else Button(modifier = Modifier.fillMaxWidth(), enabled = idle && devices.any { it.address == selected }, onClick = {
                if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED)
                    notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                else startSleepTrial()
            }) { Text("Uyku geciktirmeyi dene · 12 dk") }
            val sleepReport = SleepTrialService.latestReport
            if (sleepReport.isNotEmpty()) {
                Text(try { org.json.JSONObject(sleepReport).optString("status") } catch (_: Exception) { "Uyku raporu hazır" }, style = MaterialTheme.typography.bodySmall)
                TextButton(enabled = idle, onClick = {
                    report = sleepReport
                    prefs.edit().putString("report", report).apply()
                }) { Text("Uyku raporunu göster") }
            }
            OutlinedButton(enabled = report.isNotEmpty() && idle, onClick = { export.launch("DYO-Keyboard-Analiz.json") }) { Text("Teknik raporu kaydet") }
            Text("Rapor seçili adresi, ayrı InputDevice gözlemlerini ve zaman damgasını içerir. İnternet kullanılmaz. Işık gözleminde standart Bluetooth bildirim aboneliği kurulur; LED komutu gönderilmez.", style = MaterialTheme.typography.bodySmall)
        }
    }
    @Composable private fun Report(modifier: Modifier) {
        LazyColumn(modifier.background(Color(0xFF0A0A0A)).padding(20.dp)) { item { ReportText() } }
    }
    @Composable private fun ReportText() {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Teknik bulgular", style = MaterialTheme.typography.titleLarge)
            Text("Son rapor, kendi deviceName/address/timestampUtc alanlarında belirtilen cihaza aittir.", style = MaterialTheme.typography.bodySmall)
            SelectionContainer { Text(report.ifEmpty { "Henüz analiz yapılmadı. Klavyeyi seçip Donanımı incele düğmesine dokun." }, style = MaterialTheme.typography.bodySmall) }
        }
    }
    override fun onStop() {
        if(::analyzer.isInitialized && busy) { if(observing) observer.cancel() else analyzer.cancel(); busy = false; status = "Uygulama arka plana geçti; analiz durduruldu. Rapor kısmi olabilir." }
        super.onStop()
    }
    override fun onDestroy() { if(::analyzer.isInitialized) analyzer.cancel(); if(::observer.isInitialized) observer.cancel(); super.onDestroy() }
}

