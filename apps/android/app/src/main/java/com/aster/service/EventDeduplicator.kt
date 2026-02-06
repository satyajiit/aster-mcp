package com.aster.service

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

/**
 * Deduplicates event forwarding to prevent notification spam.
 *
 * Handles three dedup layers:
 * 1. SMS broadcast vs notification — SMS_RECEIVED broadcast is canonical, suppress duplicate
 *    notifications from known SMS apps within a 5s window.
 * 2. Notification content dedup — Same package + title + text within a 30s window is suppressed.
 * 3. Ongoing notification tracking — Ongoing (sticky) notifications only forwarded once per key;
 *    subsequent updates are suppressed until the notification is removed and reposted.
 */
object EventDeduplicator {

    private const val SMS_DEDUP_WINDOW_MS = 5000L
    private const val NOTIFICATION_DEDUP_WINDOW_MS = 30000L
    private const val CLEANUP_INTERVAL_MS = 5000L

    // SMS broadcast -> notification dedup
    private val smsEntries = ConcurrentHashMap<String, Long>()

    // Notification content dedup: hash(pkg+title+text) -> timestamp
    private val notificationEntries = ConcurrentHashMap<String, Long>()

    // Ongoing notification tracking: sbn.key -> true (forwarded once)
    private val ongoingNotificationKeys = ConcurrentHashMap<String, Boolean>()

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
     */
    fun recordSms(sender: String, body: String) {
        val key = body.trim().hashCode().toString()
        smsEntries[key] = System.currentTimeMillis()
    }

    /**
     * Check if a notification is a duplicate of a recently received SMS.
     */
    fun isDuplicateOfSms(packageName: String, text: String?, sender: String?): Boolean {
        if (packageName !in knownSmsPackages) return false
        if (text.isNullOrBlank()) return false

        val now = System.currentTimeMillis()
        val key = text.trim().hashCode().toString()
        val recordedAt = smsEntries[key] ?: return false
        return (now - recordedAt) < SMS_DEDUP_WINDOW_MS
    }

    /**
     * Check if a notification with the same content was recently forwarded.
     * Returns true if the notification should be suppressed.
     * If not suppressed, records it for future dedup.
     */
    fun isDuplicateNotification(packageName: String, title: String, text: String): Boolean {
        val key = "$packageName|$title|$text".hashCode().toString()
        val now = System.currentTimeMillis()
        val lastSeen = notificationEntries[key]
        if (lastSeen != null && (now - lastSeen) < NOTIFICATION_DEDUP_WINDOW_MS) {
            return true
        }
        notificationEntries[key] = now
        return false
    }

    /**
     * Check if an ongoing (sticky) notification has already been forwarded.
     * Returns true if it should be suppressed (already forwarded).
     * First occurrence is allowed through and tracked.
     */
    fun isOngoingAlreadyForwarded(notificationKey: String): Boolean {
        return ongoingNotificationKeys.putIfAbsent(notificationKey, true) != null
    }

    /**
     * Called when an ongoing notification is removed — clears the tracking
     * so it will be forwarded again if reposted.
     */
    fun clearOngoingNotification(notificationKey: String) {
        ongoingNotificationKeys.remove(notificationKey)
    }

    /**
     * Start periodic cleanup of stale entries.
     */
    fun startCleanup(scope: CoroutineScope) {
        cleanupJob?.cancel()
        cleanupJob = scope.launch {
            while (isActive) {
                delay(CLEANUP_INTERVAL_MS)
                val now = System.currentTimeMillis()

                val smsCutoff = now - SMS_DEDUP_WINDOW_MS
                smsEntries.entries.removeAll { it.value < smsCutoff }

                val notifCutoff = now - NOTIFICATION_DEDUP_WINDOW_MS
                notificationEntries.entries.removeAll { it.value < notifCutoff }
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
        notificationEntries.clear()
        ongoingNotificationKeys.clear()
    }
}
