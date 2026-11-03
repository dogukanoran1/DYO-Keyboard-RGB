package com.dogukan.keyboardrgb

/** A bounded experiment. Successful GATT reads never imply that LEDs or HID stayed awake. */
class SleepTrial(
    private val port: Port,
    private val scheduler: Scheduler,
    private val onUpdate: (Snapshot) -> Unit
) {
    interface Port {
        interface Listener {
            fun ready()
            fun readCompleted(status: Int, value: ByteArray)
            fun disconnected(reason: String)
        }
        fun connect(listener: Listener)
        fun readManufacturer(): Boolean
        fun close()
    }

    fun interface Cancellation { fun cancel() }
    interface Scheduler {
        fun nowMs(): Long
        fun after(delayMs: Long, task: () -> Unit): Cancellation
    }

    enum class Phase { CONNECTING, RUNNING, COMPLETED, STOPPED, CONNECTION_ERROR, READ_ERROR, READ_TIMEOUT, SETUP_TIMEOUT }
    data class Snapshot(
        val phase: Phase,
        val status: String,
        val elapsedMs: Long = 0,
        val readsStarted: Int = 0,
        val successfulReads: Int = 0,
        val lastValueHex: String = ""
    ) {
        val running: Boolean get() = phase == Phase.CONNECTING || phase == Phase.RUNNING
    }

    private var snapshot = Snapshot(Phase.STOPPED, "Henüz başlatılmadı")
    private var generation = 0
    private var startedAt = 0L
    private var awaitingRead = false
    private val jobs = mutableListOf<Cancellation>()
    private var setupTimeout: Cancellation? = null
    private var readTimeout: Cancellation? = null
    val running: Boolean get() = snapshot.running

    fun start() {
        if (running) return
        generation++
        val token = generation
        startedAt = scheduler.nowMs()
        awaitingRead = false
        jobs.clear()
        snapshot = Snapshot(Phase.CONNECTING, "Klavyeye bağlanıyor")
        publish()
        setupTimeout = schedule(SETUP_TIMEOUT_MS) {
            if (current(token)) finish(Phase.SETUP_TIMEOUT, "20 saniye içinde bağlantı ve servis keşfi tamamlanamadı")
        }
        try {
            port.connect(object : Port.Listener {
                override fun ready() {
                    if (!current(token) || snapshot.phase != Phase.CONNECTING) return
                    setupTimeout?.cancel()
                    startedAt = scheduler.nowMs()
                    snapshot = snapshot.copy(phase = Phase.RUNNING, status = "12 dakikalık uyku deneyi sürüyor")
                    publish()
                    schedule(DURATION_MS) {
                        if (current(token)) finish(Phase.COMPLETED, "Deney tamamlandı; ışık ve klavye uykusu fiziksel olarak değerlendirilmelidir")
                    }
                    read(token)
                }

                override fun readCompleted(status: Int, value: ByteArray) {
                    if (!current(token) || !awaitingRead) return
                    awaitingRead = false
                    readTimeout?.cancel()
                    if (status != 0) {
                        finish(Phase.READ_ERROR, "Klavyeden okuma reddedildi: $status")
                        return
                    }
                    snapshot = snapshot.copy(successfulReads = snapshot.successfulReads + 1,
                        lastValueHex = value.take(128).joinToString("") { "%02X".format(it.toInt() and 255) })
                    publish()
                    schedule(INTERVAL_MS) { if (current(token)) read(token) }
                }

                override fun disconnected(reason: String) {
                    if (current(token)) finish(Phase.CONNECTION_ERROR, reason)
                }
            })
        } catch (e: Exception) {
            if (current(token)) finish(Phase.CONNECTION_ERROR, "Bağlantı başlatılamadı: ${e.message}")
        }
    }

    fun stop(reason: String = "Kullanıcı tarafından durduruldu") {
        if (running) finish(Phase.STOPPED, reason)
    }

    private fun current(token: Int) = token == generation && running
    private fun schedule(delayMs: Long, task: () -> Unit): Cancellation =
        scheduler.after(delayMs, task).also { jobs.add(it) }

    private fun read(token: Int) {
        if (!current(token) || snapshot.phase != Phase.RUNNING || awaitingRead) return
        awaitingRead = true
        snapshot = snapshot.copy(readsStarted = snapshot.readsStarted + 1)
        readTimeout = schedule(READ_TIMEOUT_MS) {
            if (current(token) && awaitingRead) finish(Phase.READ_TIMEOUT, "Klavyeden 5 saniye içinde okuma yanıtı gelmedi")
        }
        try {
            if (!port.readManufacturer()) finish(Phase.READ_ERROR, "Standart üretici bilgisi okuması başlatılamadı")
        } catch (e: Exception) {
            if (current(token)) finish(Phase.READ_ERROR, "Okuma hatası: ${e.message}")
        }
    }

    private fun publish() {
        snapshot = snapshot.copy(elapsedMs = (scheduler.nowMs() - startedAt).coerceAtLeast(0))
        onUpdate(snapshot)
    }

    private fun finish(phase: Phase, reason: String) {
        if (!running) return
        generation++
        awaitingRead = false
        jobs.forEach { it.cancel() }
        jobs.clear()
        snapshot = snapshot.copy(phase = phase, status = reason)
        try { port.close() } catch (_: Exception) { /* Report the experiment result even if cleanup fails. */ }
        publish()
    }

    companion object {
        const val DURATION_MS = 12 * 60 * 1000L
        const val INTERVAL_MS = 15 * 1000L
        const val READ_TIMEOUT_MS = 5 * 1000L
        const val SETUP_TIMEOUT_MS = 20 * 1000L
    }
}
