package com.aster.service

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

/**
 * Deduplicates SMS broadcast events against notification events.
 *
 * When an SMS arrives, Android fires both an SMS_RECEIVED broadcast AND the messaging
 * app posts a notification. We treat the SMS broadcast as canonical and suppress
 * the duplicate notification from known SMS apps within a 5-second window.
 */
object EventDeduplicator {

    private const val DEDUP_WINDOW_MS = 5000L
    private const val CLEANUP_INTERVAL_MS = 1000L

    private val smsEntries = ConcurrentHashMap<String, Long>()
    private var cleanupJob: Job? = null

    private val knownSmsPackages = setOf(
        "com.google.android.apps.messaging",
        "com.samsung.android.messaging",
        "com.android.mms",
        "com.android.messaging",
        "com.oneplus.mms",
        "com.xiaomi.mms"
    )

    /**
     * Record that an SMS was received. Call BEFORE forwarding the SMS event.
     * Key is body hash only — sender format differs between SMS broadcast (phone number)
     * and notification (contact name), so we match on body text alone.
     */
    fun recordSms(sender: String, body: String) {
        val key = body.trim().hashCode().toString()
        smsEntries[key] = System.currentTimeMillis()
    }

    /**
     * Check if a notification is a duplicate of a recently received SMS.
     * Returns true if the notification should be suppressed.
     */
    fun isDuplicateOfSms(packageName: String, text: String?, sender: String?): Boolean {
        if (packageName !in knownSmsPackages) return false
        if (text.isNullOrBlank()) return false

        val now = System.currentTimeMillis()
        val key = text.trim().hashCode().toString()
        val recordedAt = smsEntries[key] ?: return false
        return (now - recordedAt) < DEDUP_WINDOW_MS
    }

    /**
     * Start periodic cleanup of stale entries.
     */
    fun startCleanup(scope: CoroutineScope) {
        cleanupJob?.cancel()
        cleanupJob = scope.launch {
            while (isActive) {
                delay(CLEANUP_INTERVAL_MS)
                val cutoff = System.currentTimeMillis() - DEDUP_WINDOW_MS
                smsEntries.entries.removeAll { it.value < cutoff }
            }
        }
    }

    /**
     * Stop cleanup and clear all entries.
     */
    fun stopCleanup() {
        cleanupJob?.cancel()
        cleanupJob = null
        smsEntries.clear()
    }
}
