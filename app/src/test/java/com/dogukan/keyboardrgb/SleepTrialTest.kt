package com.dogukan.keyboardrgb

import org.junit.Assert.*
import org.junit.Test

class SleepTrialTest {
    private class Clock : SleepTrial.Scheduler {
        data class Job(val at: Long, val task: () -> Unit, var cancelled: Boolean = false)
        var time = 0L
        private val jobs = mutableListOf<Job>()
        override fun nowMs() = time
        override fun after(delayMs: Long, task: () -> Unit): SleepTrial.Cancellation {
            val job = Job(time + delayMs, task)
            jobs.add(job)
            return SleepTrial.Cancellation { job.cancelled = true }
        }
        fun advance(ms: Long) {
            val end = time + ms
            while (true) {
                val job = jobs.filter { !it.cancelled && it.at <= end }.minByOrNull { it.at } ?: break
                jobs.remove(job)
                time = job.at
                job.task()
            }
            time = end
        }
    }
    private class Port : SleepTrial.Port {
        lateinit var listener: SleepTrial.Port.Listener
        var reads = 0
        var closes = 0
        var automaticResponses = false
        var acceptsRead = true
        override fun connect(listener: SleepTrial.Port.Listener) { this.listener = listener }
        override fun readManufacturer(): Boolean {
            reads++
            if (automaticResponses) listener.readCompleted(0, "Hailuck".toByteArray())
            return acceptsRead
        }
        override fun close() { closes++ }
    }
    private class Fixture {
        val clock = Clock()
        val port = Port()
        val updates = mutableListOf<SleepTrial.Snapshot>()
        val trial = SleepTrial(port, clock, updates::add)
        val last get() = updates.last()
        fun ready() { trial.start(); port.listener.ready() }
    }

    @Test fun successfulReadsDoNotExtendTheTwelveMinuteDeadline() {
        val f = Fixture()
        f.port.automaticResponses = true
        f.ready()
        f.clock.advance(SleepTrial.DURATION_MS)
        assertEquals(SleepTrial.Phase.COMPLETED, f.last.phase)
        assertEquals(48, f.port.reads)
        assertEquals(48, f.last.successfulReads)
        assertEquals(SleepTrial.DURATION_MS, f.last.elapsedMs)
        assertEquals(1, f.port.closes)
        f.clock.advance(SleepTrial.DURATION_MS)
        assertEquals(48, f.port.reads)
    }

    @Test fun missingReadResponseStopsWithoutStartingOverlappingRequests() {
        val f = Fixture()
        f.ready()
        f.clock.advance(SleepTrial.READ_TIMEOUT_MS)
        assertEquals(SleepTrial.Phase.READ_TIMEOUT, f.last.phase)
        assertEquals(1, f.port.reads)
        assertEquals(0, f.last.successfulReads)
        f.clock.advance(SleepTrial.DURATION_MS)
        assertEquals(1, f.port.reads)
    }

    @Test fun unavailableServiceCannotLeaveTheExperimentRunning() {
        val f = Fixture()
        f.trial.start()
        f.clock.advance(SleepTrial.SETUP_TIMEOUT_MS)
        assertEquals(SleepTrial.Phase.SETUP_TIMEOUT, f.last.phase)
        assertEquals(0, f.port.reads)
        assertEquals(1, f.port.closes)
    }

    @Test fun stopInvalidatesQueuedResponsesAndAllFutureReads() {
        val f = Fixture()
        f.ready()
        val listener = f.port.listener
        f.trial.stop()
        val stoppedUpdates = f.updates.size
        listener.readCompleted(0, byteArrayOf(1))
        listener.ready()
        listener.disconnected("late")
        f.clock.advance(SleepTrial.DURATION_MS)
        assertEquals(stoppedUpdates, f.updates.size)
        assertEquals(SleepTrial.Phase.STOPPED, f.last.phase)
        assertEquals(1, f.port.reads)
        assertEquals(1, f.port.closes)
    }

    @Test fun rejectedReadIsReportedAsFailureRatherThanKeepAwakeSuccess() {
        val f = Fixture()
        f.port.acceptsRead = false
        f.ready()
        assertEquals(SleepTrial.Phase.READ_ERROR, f.last.phase)
        assertEquals(0, f.last.successfulReads)
        assertEquals(1, f.last.readsStarted)
    }

    @Test fun failedReadCallbackStopsTheTrial() {
        val f = Fixture()
        f.ready()
        f.port.listener.readCompleted(5, byteArrayOf())
        assertEquals(SleepTrial.Phase.READ_ERROR, f.last.phase)
        assertEquals(0, f.last.successfulReads)
        f.clock.advance(SleepTrial.INTERVAL_MS)
        assertEquals(1, f.port.reads)
    }

    @Test fun duplicateStartAndReadyDoNotResetTheDeadline() {
        val f = Fixture()
        f.port.automaticResponses = true
        f.ready()
        f.clock.advance(10_000)
        f.trial.start()
        f.port.listener.ready()
        f.clock.advance(SleepTrial.DURATION_MS - 10_000)
        assertEquals(SleepTrial.Phase.COMPLETED, f.last.phase)
        assertEquals(48, f.port.reads)
    }

    @Test fun callbacksFromAnEarlierRunCannotAffectANewRun() {
        val f = Fixture()
        f.ready()
        val old = f.port.listener
        f.trial.stop()
        f.trial.start()
        old.ready()
        old.readCompleted(0, byteArrayOf(1))
        old.disconnected("old connection")
        assertEquals(SleepTrial.Phase.CONNECTING, f.last.phase)
        assertEquals(0, f.last.successfulReads)
        assertEquals(1, f.port.reads)
        f.port.listener.ready()
        assertEquals(2, f.port.reads)
    }
}
