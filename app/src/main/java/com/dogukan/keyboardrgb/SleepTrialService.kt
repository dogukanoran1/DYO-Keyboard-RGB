package com.dogukan.keyboardrgb

import android.Manifest
import android.annotation.SuppressLint
import android.app.*
import android.bluetooth.*
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.json.JSONObject
import java.util.UUID

/** User-started, read-only, twelve-minute test; no manufacturer commands or HID writes. */
@SuppressLint("MissingPermission")
class SleepTrialService : Service() {
    private var trial: SleepTrial? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private var address = ""
    private var deviceName = ""
    private var startedUtcMs = 0L

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            trial?.stop()
            if (trial == null) stopSelf()
            return START_NOT_STICKY
        }
        if (running) return START_NOT_STICKY
        startedUtcMs = System.currentTimeMillis()
        address = intent?.getStringExtra(EXTRA_ADDRESS).orEmpty()
        try {
            check(checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                "Yakındaki cihazlar izni gerekli"
            }
            val adapter = getSystemService(BluetoothManager::class.java)?.adapter
                ?: error("Bluetooth adaptörü bulunamadı")
            check(adapter.isEnabled) { "Bluetooth kapalı" }
            val device = adapter.bondedDevices.firstOrNull { it.address == address }
                ?: error("Seçili eşleştirilmiş klavye bulunamadı")
            check(device.type != BluetoothDevice.DEVICE_TYPE_CLASSIC) { "Bu deney BLE bağlantısı gerektirir" }
            deviceName = device.name ?: "Klavyem"
            val notifications = requireNotNull(getSystemService(NotificationManager::class.java))
            notifications.createNotificationChannel(NotificationChannel(CHANNEL, "Klavye uyku deneyi", NotificationManager.IMPORTANCE_LOW))
            startForeground(NOTIFICATION_ID, notification("Klavyeye bağlanıyor"), ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE)
            val scheduler = MainThreadScheduler()
            trial = SleepTrial(GattReadPort(applicationContext, device), scheduler) { state ->
                save(state)
                if (state.running) {
                    notifications.notify(NOTIFICATION_ID, notification("${state.successfulReads} okuma · ${state.elapsedMs / 1000} saniye"))
                } else {
                    releaseWakeLock()
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf()
                }
            }
            wakeLock = requireNotNull(getSystemService(PowerManager::class.java)).newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                "DYOKeyboard:SleepTrial").apply {
                // Bounded even if Android kills a callback or the service cannot clean up.
                acquire(SleepTrial.DURATION_MS + SleepTrial.SETUP_TIMEOUT_MS + 30_000L)
            }
            trial?.start()
        } catch (e: Exception) {
            save(SleepTrial.Snapshot(SleepTrial.Phase.CONNECTION_ERROR, "Deney başlatılamadı: ${e.message}"))
            releaseWakeLock()
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
        return START_NOT_STICKY
    }

    private fun save(state: SleepTrial.Snapshot) {
        running = state.running
        latestReport = JSONObject()
            .put("reportType", "read_only_sleep_trial").put("appVersion", "0.3.0")
            .put("deviceName", deviceName).put("address", address).put("startedUtcMs", startedUtcMs)
            .put("updatedUtcMs", System.currentTimeMillis()).put("running", state.running)
            .put("phase", state.phase.name).put("status", state.status).put("elapsedMs", state.elapsedMs)
            .put("plannedDurationMs", SleepTrial.DURATION_MS).put("intervalMs", SleepTrial.INTERVAL_MS)
            .put("readsStarted", state.readsStarted).put("successfulReads", state.successfulReads)
            .put("lastManufacturerHex", state.lastValueHex)
            .put("serviceUuid", DEVICE_INFORMATION.toString()).put("characteristicUuid", MANUFACTURER.toString())
            .put("characteristicWritesSent", 0).put("sleepSettingChanged", false)
            .put("sleepPrevented", JSONObject.NULL).put("backlightStayedOn", JSONObject.NULL)
            .put("interpretation", "GATT yanıtı ışığın açık kaldığını veya klavyenin HID uykusunun engellendiğini kanıtlamaz. Fiziksel gözlem gerekir.")
            .toString(2)
        getSharedPreferences(PREFS, MODE_PRIVATE).edit().putString(REPORT_KEY, latestReport).apply()
    }

    private fun notification(message: String): Notification {
        val open = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val stop = PendingIntent.getService(this, 1, Intent(this, SleepTrialService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        return Notification.Builder(this, CHANNEL)
            .setSmallIcon(android.R.drawable.stat_sys_data_bluetooth)
            .setContentTitle("DYO · 12 dakikalık uyku deneyi").setContentText(message)
            .setContentIntent(open).setOngoing(true).setOnlyAlertOnce(true)
            .addAction(Notification.Action.Builder(null, "Durdur", stop).build()).build()
    }

    private fun releaseWakeLock() {
        try { wakeLock?.let { if (it.isHeld) it.release() } } catch (_: RuntimeException) {} finally { wakeLock = null }
    }

    override fun onDestroy() {
        trial?.stop("Servis durduruldu; deney tamamlanmadı")
        releaseWakeLock()
        running = false
        super.onDestroy()
    }

    companion object {
        private const val PREFS = "sleep_trial"
        private const val REPORT_KEY = "report"
        private const val CHANNEL = "keyboard_sleep_trial"
        private const val NOTIFICATION_ID = 156
        private const val ACTION_STOP = "com.dogukan.keyboardrgb.STOP_SLEEP_TRIAL"
        const val EXTRA_ADDRESS = "address"
        private val DEVICE_INFORMATION = UUID.fromString("0000180a-0000-1000-8000-00805f9b34fb")
        private val MANUFACTURER = UUID.fromString("00002a29-0000-1000-8000-00805f9b34fb")
        var running by mutableStateOf(false)
            private set
        var latestReport by mutableStateOf("")
            private set

        fun restoreReport(context: Context) {
            if (running) return
            val saved = context.getSharedPreferences(PREFS, MODE_PRIVATE).getString(REPORT_KEY, "").orEmpty()
            latestReport = try {
                if (saved.isEmpty()) "" else JSONObject(saved).apply {
                    if (optBoolean("running")) {
                        put("running", false).put("phase", "INTERRUPTED")
                            .put("status", "Önceki deney kesildi; uygulama süreci sonlanmış olabilir")
                    }
                }.toString(2)
            } catch (_: Exception) { "" }
            if (latestReport != saved) context.getSharedPreferences(PREFS, MODE_PRIVATE).edit().putString(REPORT_KEY, latestReport).apply()
        }
    }

    private class MainThreadScheduler : SleepTrial.Scheduler {
        private val handler = Handler(Looper.getMainLooper())
        override fun nowMs() = SystemClock.elapsedRealtime()
        override fun after(delayMs: Long, task: () -> Unit): SleepTrial.Cancellation {
            val runnable = Runnable { task() }
            handler.postDelayed(runnable, delayMs)
            return SleepTrial.Cancellation { handler.removeCallbacks(runnable) }
        }
    }

    private class GattReadPort(private val context: Context, private val device: BluetoothDevice) : SleepTrial.Port {
        private val main = Handler(Looper.getMainLooper())
        private var gatt: BluetoothGatt? = null
        private var listener: SleepTrial.Port.Listener? = null
        private var manufacturer: BluetoothGattCharacteristic? = null
        override fun connect(listener: SleepTrial.Port.Listener) {
            this.listener = listener
            gatt = device.connectGatt(context, false, object : BluetoothGattCallback() {
                override fun onConnectionStateChange(remote: BluetoothGatt, status: Int, newState: Int) {
                    main.post {
                        if (remote !== gatt) return@post
                        if (status != BluetoothGatt.GATT_SUCCESS || newState == BluetoothProfile.STATE_DISCONNECTED) {
                            listener.disconnected("BLE bağlantısı kesildi: $status")
                        } else if (newState == BluetoothProfile.STATE_CONNECTED) {
                            try {
                                if (!remote.discoverServices()) listener.disconnected("Servis keşfi başlatılamadı")
                            } catch (e: Exception) { listener.disconnected("Servis keşfi hatası: ${e.message}") }
                        }
                    }
                }
                override fun onServicesDiscovered(remote: BluetoothGatt, status: Int) {
                    main.post {
                        if (remote !== gatt) return@post
                        if (status != BluetoothGatt.GATT_SUCCESS) {
                            listener.disconnected("Servis keşfi başarısız: $status")
                            return@post
                        }
                        manufacturer = remote.getService(DEVICE_INFORMATION)?.getCharacteristic(MANUFACTURER)
                        if (manufacturer?.let { it.properties and BluetoothGattCharacteristic.PROPERTY_READ != 0 } != true) {
                            listener.disconnected("Standart üretici bilgisi okunamıyor; deney uygulanamadı")
                        } else listener.ready()
                    }
                }
                override fun onCharacteristicRead(remote: BluetoothGatt, characteristic: BluetoothGattCharacteristic, value: ByteArray, status: Int) {
                    val copy = value.copyOf()
                    main.post {
                        if (remote === gatt && characteristic === manufacturer) listener.readCompleted(status, copy)
                    }
                }
                @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
                override fun onCharacteristicRead(remote: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int) {
                    if (Build.VERSION.SDK_INT < 33) onCharacteristicRead(remote, characteristic, characteristic.value?.copyOf() ?: byteArrayOf(), status)
                }
            }, BluetoothDevice.TRANSPORT_LE)
            if (gatt == null) listener.disconnected("GATT istemcisi oluşturulamadı")
        }
        override fun readManufacturer(): Boolean {
            val remote = gatt ?: return false
            val characteristic = manufacturer ?: return false
            return remote.readCharacteristic(characteristic)
        }
        override fun close() {
            listener = null
            manufacturer = null
            val old = gatt
            gatt = null
            try { old?.disconnect() } catch (_: Exception) {} finally { try { old?.close() } catch (_: Exception) {} }
        }
    }
}
