package com.dogukan.keyboardrgb

import android.annotation.SuppressLint
import android.bluetooth.*
import android.os.Handler
import org.json.JSONArray
import org.json.JSONObject
import java.util.ArrayDeque

/** Reads only documented identity/descriptor metadata. Never reads live HID input reports. */
@SuppressLint("MissingPermission")
class GattMetadataReader(
    private val gatt: BluetoothGatt,
    private val handler: Handler,
    private val onChange: (JSONArray) -> Unit,
    private val onDone: (String) -> Unit
) {
    private data class Request(val label: String, val characteristic: BluetoothGattCharacteristic, val descriptor: BluetoothGattDescriptor? = null)
    private val queue = ArrayDeque<Request>()
    private val results = JSONArray()
    private var pending: Request? = null
    private var timeout: Runnable? = null
    private var stopped = false
    private fun short(uuid: java.util.UUID, id: String) = uuid.toString() == "0000$id-0000-1000-8000-00805f9b34fb"
    fun start() {
        gatt.services.forEach { service ->
            service.characteristics.forEach { ch ->
                val identity = if(short(service.uuid, "180a")) when {
                    short(ch.uuid, "2a29") -> "manufacturerName"
                    short(ch.uuid, "2a24") -> "modelNumber"
                    short(ch.uuid, "2a26") -> "firmwareRevision"
                    short(ch.uuid, "2a27") -> "hardwareRevision"
                    short(ch.uuid, "2a28") -> "softwareRevision"
                    short(ch.uuid, "2a50") -> "pnpId"
                    else -> null
                } else null
                val hid = short(service.uuid, "1812")
                val label = identity ?: if(hid && short(ch.uuid, "2a4b")) "hidReportMap" else null
                if(label != null && ch.properties and BluetoothGattCharacteristic.PROPERTY_READ != 0) queue.add(Request(label, ch))
                // Report Reference contains only report ID/type, not typed keys. Do not read Report values (2A4D).
                if(hid && short(ch.uuid, "2a4d")) ch.descriptors.filter { short(it.uuid, "2908") }
                    .forEach { queue.add(Request("hidReportReference", ch, it)) }
            }
        }
        if(queue.size > 32) { queue.clear(); stopWith("Beklenmeyen sayıda meta veri; okuma yapılmadı"); return }
        next()
    }
    private fun record(request: Request, status: String, value: ByteArray? = null) {
        val row = JSONObject().put("label", request.label)
            .put("serviceUuid", request.characteristic.service.uuid.toString())
            .put("serviceInstanceId", request.characteristic.service.instanceId)
            .put("characteristicUuid", request.characteristic.uuid.toString())
            .put("characteristicInstanceId", request.characteristic.instanceId)
            .put("status", status)
        request.descriptor?.let { row.put("descriptorUuid", it.uuid.toString()) }
        if(value != null) {
            row.put("length", value.size).put("hex", MetadataValues.hex(value))
            when(request.label) {
                "pnpId" -> MetadataValues.pnp(value)?.let {
                    row.put("vendorIdSource", it.source).put("vendorId", it.vendorId)
                        .put("productId", it.productId).put("productVersion", it.version)
                        .put("vendorIdNamespace", when(it.source) { 1 -> "Bluetooth SIG"; 2 -> "USB-IF"; else -> "Unknown" })
                } ?: row.put("decodeError", "PnP ID tam 7 bayt olmalı")
                "hidReportReference" -> if(value.size == 2) row.put("reportId", value[0].toInt() and 255)
                    .put("reportType", when(value[1].toInt() and 255) { 1 -> "Input"; 2 -> "Output"; 3 -> "Feature"; else -> "Unknown" })
                "hidReportMap" -> row.put("interpretation", "Ham HID tanımı; RGB desteği sonucu çıkarılmadı")
                else -> row.put("text", value.toString(Charsets.UTF_8).trimEnd('\u0000'))
            }
        }
        results.put(row); onChange(results)
    }
    private fun next() {
        if(stopped) return
        val request = queue.pollFirst() ?: run { stopWith("Meta veri okuma sırası tamamlandı; tekil hatalar sonuçlarda"); return }
        pending = request
        try {
            val started = if(request.descriptor == null) gatt.readCharacteristic(request.characteristic) else gatt.readDescriptor(request.descriptor)
            if(!started) { pending = null; record(request, "NOT_STARTED"); handler.post { next() }; return }
            timeout = Runnable {
                if(!stopped && pending === request) {
                    record(request, "TIMEOUT_4_SECONDS")
                    // Never start another request while a timed-out GATT operation might still be in flight.
                    queue.forEach { record(it, "NOT_ATTEMPTED_AFTER_TIMEOUT") }; queue.clear()
                    stopWith("Okuma zaman aşımı; bağlantı kapatılacak, tekrar denenmedi")
                }
            }.also { handler.postDelayed(it, 4000) }
        } catch(e: SecurityException) {
            pending = null; record(request, "ACCESS_DENIED: ${e.message}")
            handler.post { next() }
        } catch(e: Exception) {
            record(request, "ERROR: ${e.javaClass.simpleName}: ${e.message}")
            stopWith("Meta veri okuması hata nedeniyle durdu")
        }
    }
    fun characteristicRead(characteristic: BluetoothGattCharacteristic, value: ByteArray, status: Int) {
        val request = pending ?: return
        if(stopped || request.descriptor != null || request.characteristic !== characteristic) return
        complete(request, value, status)
    }
    fun descriptorRead(descriptor: BluetoothGattDescriptor, value: ByteArray, status: Int) {
        val request = pending ?: return
        if(stopped || request.descriptor !== descriptor) return
        complete(request, value, status)
    }
    private fun complete(request: Request, value: ByteArray, status: Int) {
        timeout?.let { handler.removeCallbacks(it) }; timeout = null; pending = null
        record(request, if(status == BluetoothGatt.GATT_SUCCESS) "SUCCESS" else "GATT_STATUS_$status", if(status == BluetoothGatt.GATT_SUCCESS) value else null)
        handler.post { next() }
    }
    private fun stopWith(reason: String) { stop(); onDone(reason) }
    fun stop() { stopped = true; timeout?.let { handler.removeCallbacks(it) }; timeout = null; pending = null; queue.clear() }
}
