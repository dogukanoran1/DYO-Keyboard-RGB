package com.dogukan.keyboardrgb

import android.annotation.SuppressLint
import android.bluetooth.*
import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/** Bounded observation of the selected device's non-HID notification endpoint. */
@SuppressLint("MissingPermission")
class LightingObserver(private val context: Context) {
    private val handler = Handler(Looper.getMainLooper())
    private var gatt: BluetoothGatt? = null
    private var callback: ((String, Boolean) -> Unit)? = null
    private var report: JSONObject? = null
    private val events = JSONArray()
    private var generation = 0
    private var active = false
    private var subscribedAt = 0L
    private var subscribeRequested = false
    private var subscribeSucceeded = false
    private val serviceId = UUID.fromString("0000ffe0-0000-1000-8000-00805f9b34fb")
    private val notifyId = UUID.fromString("0000ffb4-0000-1000-8000-00805f9b34fb")
    private val cccdId = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
    private fun emit(running: Boolean) { callback?.invoke(report?.toString(2) ?: "", running) }
    private fun finish(reason: String) {
        if (!active) return
        active = false
        generation++
        handler.removeCallbacksAndMessages(null)
        report?.put("status", reason)?.put("running", false)?.put("notificationCount", events.length())
            ?.put("subscriptionWriteRequested", subscribeRequested)?.put("subscriptionWriteSucceeded", subscribeSucceeded)
            ?.put("ledCommandsSent", 0)
            ?.put("cleanup", "GATT istemcisi kapatıldı; CCCD durumunun cihaz tarafından sıfırlanması doğrulanmadı")
        val old = gatt; gatt = null
        try { old?.disconnect() } catch (_: Exception) {}
        try { old?.close() } catch (_: Exception) {}
        emit(false)
    }
    fun cancel() = finish("Kullanıcı veya yaşam döngüsü tarafından durduruldu")
    fun start(device: BluetoothDevice, onChange: (String, Boolean) -> Unit) {
        if (active) return
        generation++; val token = generation
        active = true; callback = onChange; subscribeRequested = false; subscribeSucceeded = false
        while (events.length() > 0) events.remove(0)
        report = JSONObject().put("reportType", "lighting_notification_observation")
            .put("appVersion", "0.2.0").put("observerVersion", 1).put("deviceName", try { device.name ?: "Adsız" } catch (_: Exception) { "Bilinmiyor" })
            .put("address", device.address).put("timestampUtcMs", System.currentTimeMillis())
            .put("serviceUuid", serviceId.toString()).put("characteristicUuid", notifyId.toString())
            .put("running", true).put("status", "Bağlanıyor")
            .put("ledCommandsSent", 0).put("events", events)
            .put("interpretation", "Bildirim içeriği bilinmiyor; bildirim gelmesi LED kontrolünü kanıtlamaz.")
        emit(true)
        handler.postDelayed({ if (active && token == generation) finish("Bağlantı veya abonelik zaman aşımı") }, 20000)
        try {
            gatt = device.connectGatt(context, false, object : BluetoothGattCallback() {
                override fun onConnectionStateChange(remote: BluetoothGatt, status: Int, state: Int) {
                    handler.post {
                        if (!active || token != generation || remote !== gatt) return@post
                        if (status != BluetoothGatt.GATT_SUCCESS || state == BluetoothProfile.STATE_DISCONNECTED) finish("Bağlantı kesildi veya hata: $status")
                        else if (state == BluetoothProfile.STATE_CONNECTED) try {
                            report?.put("status", "Servisler aranıyor"); emit(true)
                            if (!remote.discoverServices()) finish("Servis keşfi başlatılamadı")
                        } catch (e: Exception) { finish("Servis keşfi hatası: ${e.message}") }
                    }
                }
                override fun onServicesDiscovered(remote: BluetoothGatt, status: Int) {
                    handler.post {
                        if (!active || token != generation || remote !== gatt) return@post
                        if (status != BluetoothGatt.GATT_SUCCESS) { finish("Servis keşfi hatası: $status"); return@post }
                        val ch = remote.getService(serviceId)?.getCharacteristic(notifyId)
                        val cccd = ch?.getDescriptor(cccdId)
                        if (ch == null || cccd == null || ch.properties and BluetoothGattCharacteristic.PROPERTY_NOTIFY == 0) {
                            finish("Beklenen bildirim kanalı bulunamadı"); return@post
                        }
                        try {
                            if (!remote.setCharacteristicNotification(ch, true)) { finish("Bildirim kaydı başlatılamadı"); return@post }
                            // Standard GATT CCCD subscription; no data is written to FFB3 or any lighting endpoint.
                            subscribeRequested = true
                            val started = if (Build.VERSION.SDK_INT >= 33) remote.writeDescriptor(cccd, BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE) == BluetoothStatusCodes.SUCCESS
                                else { @Suppress("DEPRECATION") cccd.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE; @Suppress("DEPRECATION") remote.writeDescriptor(cccd) }
                            if (!started) finish("Bildirim aboneliği başlatılamadı")
                        } catch (e: Exception) { finish("Bildirim aboneliği hatası: ${e.javaClass.simpleName}: ${e.message}") }
                    }
                }
                override fun onDescriptorWrite(remote: BluetoothGatt, descriptor: BluetoothGattDescriptor, status: Int) {
                    handler.post {
                        if (!active || token != generation || remote !== gatt || descriptor.uuid != cccdId || descriptor.characteristic.uuid != notifyId) return@post
                        if (status != BluetoothGatt.GATT_SUCCESS) { finish("Bildirim aboneliği reddedildi: $status"); return@post }
                        subscribeSucceeded = true; subscribedAt = android.os.SystemClock.elapsedRealtime()
                        handler.removeCallbacksAndMessages(null)
                        report?.put("status", "Gözlem başladı: Klavyede ışık ve renk tuşlarına sırayla bas. Normal yazı yazma."); emit(true)
                        handler.postDelayed({ if (active && token == generation) finish("45 saniyelik gözlem tamamlandı") }, 45000)
                    }
                }
                override fun onCharacteristicChanged(remote: BluetoothGatt, ch: BluetoothGattCharacteristic, value: ByteArray) {
                    val copy = value.copyOf()
                    handler.post {
                        if (!active || token != generation || remote !== gatt || ch.uuid != notifyId || ch.service.uuid != serviceId || !subscribeSucceeded) return@post
                        if (events.length() >= 100) { finish("100 bildirim sınırına ulaşıldı"); return@post }
                        events.put(JSONObject().put("elapsedMs", android.os.SystemClock.elapsedRealtime() - subscribedAt)
                            .put("length", copy.size).put("hex", MetadataValues.hex(copy.take(512).toByteArray())))
                        emit(true)
                    }
                }
                @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
                override fun onCharacteristicChanged(remote: BluetoothGatt, ch: BluetoothGattCharacteristic) {
                    if (Build.VERSION.SDK_INT < 33) onCharacteristicChanged(remote, ch, ch.value?.copyOf() ?: byteArrayOf())
                }
            }, BluetoothDevice.TRANSPORT_LE)
            if (gatt == null) finish("GATT istemcisi oluşturulamadı")
        } catch (e: Exception) { finish("Bağlantı hatası: ${e.javaClass.simpleName}: ${e.message}") }
    }
}
