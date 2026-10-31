package com.dogukan.keyboardrgb

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Context
import android.view.InputDevice
import org.json.JSONArray
import org.json.JSONObject

@SuppressLint("MissingPermission")
class BluetoothDeviceManager(context: Context) {
    val adapter = context.getSystemService(BluetoothManager::class.java)?.adapter
    fun pairedDevices(): List<BluetoothDevice> = adapter?.takeIf { it.isEnabled }?.bondedDevices
        ?.sortedWith(compareBy<BluetoothDevice> { !((it.name ?: "").contains("keyboard", true)) }.thenBy { it.name ?: it.address }) ?: emptyList()
    fun transport(device: BluetoothDevice): String = when(device.type) {
        BluetoothDevice.DEVICE_TYPE_CLASSIC -> "Classic BR/EDR"
        BluetoothDevice.DEVICE_TYPE_LE -> "BLE"
        BluetoothDevice.DEVICE_TYPE_DUAL -> "Dual (Classic + BLE)"
        else -> "Bilinmiyor"
    }
    fun inputEvidence(): JSONArray = JSONArray().apply {
        InputDevice.getDeviceIds().asSequence().mapNotNull { InputDevice.getDevice(it) }.filter { !it.isVirtual && it.isExternal }.forEach { input ->
            put(JSONObject().put("name", input.name).put("deviceId", input.id)
                .put("vendorId", if(input.vendorId == 0) JSONObject.NULL else input.vendorId)
                .put("productId", if(input.productId == 0) JSONObject.NULL else input.productId)
                .put("sources", input.sources).put("keyboardType", input.keyboardType)
                .put("associationWithSelectedBluetoothDevice", "UNVERIFIED")
                .put("exposedLights", try { JSONArray(input.lightsManager.lights.map { light ->
                    JSONObject().put("id", light.id).put("name", light.name).put("type", light.type)
                }) } catch(e: Exception) { "Erişilemiyor: ${e.javaClass.simpleName}" }))
        }
    }
}
