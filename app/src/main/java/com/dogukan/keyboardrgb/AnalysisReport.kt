package com.dogukan.keyboardrgb

import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class AnalysisReport(val deviceName: String, val address: String, val transport: String) {
    private val root = JSONObject().put("schemaVersion", 2).put("appVersion", "0.2.0")
        .put("timestampUtc", SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }.format(Date()))
        .put("deviceName", deviceName).put("address", address).put("reportedTransport", transport)
        .put("lightingVerdict", "UNKNOWN_NOT_VERIFIED").put("lightingEvidence", UnverifiedProtocol.evidence)
        .put("writesSent", 0).put("limitations", JSONArray(listOf(
            "Eşleştirilmiş olmak bağlı olmak değildir. ACL olayları HID profil durumunu kanıtlamaz.",
            "InputDevice listesi ayrı gözlemdir; Bluetooth adresiyle kesin eşleştirme yapılmaz.",
            "SDP sonuçları önbellekten gelebilir. GATT keşif hatası donanımın desteklemediğini kanıtlamaz.",
            "Yazılabilir karakteristik RGB desteğini; 7 renk bağımsız LED bölgelerini kanıtlamaz.",
            "BluetoothHidDevice API tabletin HID aygıtı olması içindir; klavyenin HID host kontrolü değildir."
        )))
    fun put(key: String, value: Any): AnalysisReport { root.put(key, value); return this }
    fun json(): String = root.toString(2)
}
