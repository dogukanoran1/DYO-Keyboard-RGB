package com.dogukan.keyboardrgb

import android.annotation.SuppressLint
import android.bluetooth.*
import android.content.*
import android.os.Handler
import android.os.Looper
import android.os.ParcelUuid
import org.json.JSONArray
import org.json.JSONObject

/** Only service discovery and metadata observation; no characteristic/descriptor writes or HID reports. */
@SuppressLint("MissingPermission", "UnspecifiedRegisterReceiverFlag")
class KeyboardHardwareAnalyzer(private val context: Context, private val devices: BluetoothDeviceManager) {
    private val main = Handler(Looper.getMainLooper())
    private var gatt: BluetoothGatt? = null
    private var receiver: BroadcastReceiver? = null
    private var generation = 0
    private var report: AnalysisReport? = null
    private var notify: ((String, Boolean) -> Unit)? = null
    private var sdpTimeout: Runnable? = null
    private var bleTimeout: Runnable? = null
    private var published = false
    private var active = false
    private fun emit(busy: Boolean) { report?.let { notify?.invoke(it.json(), busy) } }
    private fun fail(message: String) { report?.put("error", message); finish() }
    private fun finish() {
        active = false
        report?.put("analysisRunning", false)
        main.removeCallbacksAndMessages(null)
        receiver?.let { try { context.unregisterReceiver(it) } catch (_: IllegalArgumentException) {} }; receiver = null
        val old = gatt; gatt = null
        try { old?.disconnect() } catch (_: Exception) {} finally { old?.close() }
        emit(false)
    }
    fun cancel() {
        generation++
        if(active) { report?.put("interrupted", true); finish() }
        notify = null
    }
    fun analyze(device: BluetoothDevice, onChange: (String, Boolean) -> Unit) {
        cancel(); val token = generation; notify = onChange; published = false; active = true
        try {
            report = AnalysisReport(device.name ?: "Adsız cihaz", device.address, devices.transport(device))
                .put("analysisRunning", true).put("interrupted", false)
                .put("bondState", device.bondState).put("deviceClass", device.bluetoothClass?.deviceClass ?: JSONObject.NULL)
                .put("connectionObservation", "Başlangıç ACL/HID bağlantı durumu public API ile doğrulanmadı")
                .put("inputDevicesObserved", devices.inputEvidence())
                .put("cachedServiceUuids", JSONArray(device.uuids?.map { it.toString() } ?: emptyList<String>()))
                .put("sdpStatus", "Sorgulanıyor").put("gattStatus", "Henüz denenmedi")
            emit(true)
            receiver = object : BroadcastReceiver() {
                override fun onReceive(c: Context, intent: Intent) {
                    if (token != generation) return
                    @Suppress("DEPRECATION") val remote = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                    if (remote?.address != device.address) return
                    when(intent.action) {
                        BluetoothDevice.ACTION_UUID -> {
                            if(published) return
                            published = true; sdpTimeout?.let { main.removeCallbacks(it) }
                            @Suppress("DEPRECATION") val values = intent.getParcelableArrayExtra(BluetoothDevice.EXTRA_UUID)?.filterIsInstance<ParcelUuid>()
                            report?.put("sdpStatus", if(values == null) "Sonuç yok veya erişim başarısız" else "ACTION_UUID alındı; önbellek olasılığı var")
                                ?.put("serviceUuids", JSONArray(values?.map { it.toString() } ?: emptyList<String>()))
                            startGatt(device, token)
                        }
                        BluetoothDevice.ACTION_ACL_CONNECTED -> { report?.put("connectionObservation", "Analiz sırasında ACL_CONNECTED gözlendi; HID bağlantısı ayrıca doğrulanmadı"); emit(true) }
                        BluetoothDevice.ACTION_ACL_DISCONNECTED -> { report?.put("connectionObservation", "Analiz sırasında ACL_DISCONNECTED gözlendi"); emit(true) }
                    }
                }
            }
            val filter = IntentFilter(BluetoothDevice.ACTION_UUID).apply { addAction(BluetoothDevice.ACTION_ACL_CONNECTED); addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED) }
            // Bluetooth broadcasts can originate from privileged Bluetooth processes outside system UID.
            if(android.os.Build.VERSION.SDK_INT >= 33) context.registerReceiver(receiver, filter, Context.RECEIVER_EXPORTED)
            else context.registerReceiver(receiver, filter)
            sdpTimeout = Runnable {
                if(token == generation && !published) { published = true; report?.put("sdpStatus", "12 saniye içinde sonuç gelmedi; destek yok sonucu çıkarılamaz"); startGatt(device, token) }
            }.also { main.postDelayed(it, 12000) }
            if(!device.fetchUuidsWithSdp()) {
                published = true; sdpTimeout?.let { main.removeCallbacks(it) }
                report?.put("sdpStatus", "Sorgu başlatılamadı; yalnızca önbellek mevcut"); startGatt(device, token)
            }
        } catch(e: Exception) { fail("${e.javaClass.simpleName}: ${e.message}") }
    }
    private fun startGatt(device: BluetoothDevice, token: Int) {
        if(token != generation) return
        if(device.type == BluetoothDevice.DEVICE_TYPE_CLASSIC) {
            report?.put("gattStatus", "Android cihaz türünü yalnızca Classic bildiriyor; BLE bağlantısı denenmedi"); finish(); return
        }
        report?.put("gattStatus", "BLE servis keşfi için bağlantı kuruluyor"); emit(true)
        bleTimeout = Runnable { if(token == generation) { report?.put("gattStatus", "20 saniye zaman aşımı; BLE/RGB desteği hakkında kesin sonuç yok"); finish() } }.also { main.postDelayed(it, 20000) }
        try {
            gatt = device.connectGatt(context, false, object : BluetoothGattCallback() {
                override fun onConnectionStateChange(remote: BluetoothGatt, status: Int, state: Int) {
                    main.post {
                        if(token != generation || gatt !== remote) return@post
                        if(status != BluetoothGatt.GATT_SUCCESS) { report?.put("gattStatus", "Bağlantı hatası: $status"); finish() }
                        else if(state == BluetoothProfile.STATE_CONNECTED) {
                            report?.put("gattStatus", "GATT bağlı; servisler keşfediliyor")
                            try { if(!remote.discoverServices()) fail("Servis keşfi başlatılamadı") else emit(true) }
                            catch(e: Exception) { fail("Servis keşfi: ${e.message}") }
                        } else if(state == BluetoothProfile.STATE_DISCONNECTED) { report?.put("gattStatus", "GATT bağlantısı kesildi; servis keşfi tamamlanmadı"); finish() }
                    }
                }
                override fun onServicesDiscovered(remote: BluetoothGatt, status: Int) {
                    main.post {
                        if(token != generation || gatt !== remote) return@post
                        try {
                            report?.put("gattStatus", if(status == BluetoothGatt.GATT_SUCCESS) "Servis keşfi tamamlandı" else "Servis keşif hatası: $status")
                            if(status == BluetoothGatt.GATT_SUCCESS) report?.put("gattServices", JSONArray(remote.services.map { service ->
                                JSONObject().put("uuid", service.uuid.toString()).put("instanceId", service.instanceId)
                                    .put("characteristics", JSONArray(service.characteristics.map { ch ->
                                        JSONObject().put("uuid", ch.uuid.toString()).put("properties", ch.properties)
                                            .put("readable", ch.properties and BluetoothGattCharacteristic.PROPERTY_READ != 0)
                                            .put("writable", ch.properties and (BluetoothGattCharacteristic.PROPERTY_WRITE or BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE) != 0)
                                            .put("descriptors", JSONArray(ch.descriptors.map { it.uuid.toString() }))
                                    }))
                            }))
                            finish()
                        } catch(e: Exception) { fail("GATT sonuçları: ${e.message}") }
                    }
                }
            }, BluetoothDevice.TRANSPORT_LE)
            if(gatt == null) fail("Android GATT istemcisini oluşturamadı")
        } catch(e: Exception) { fail("BLE bağlantısı: ${e.javaClass.simpleName}: ${e.message}") }
    }
}
