package com.aster.ipc;

import com.aster.ipc.IAsterCallback;

/**
 * The Aster companion's IPC surface.
 *
 * APPEND-ONLY. AIDL transaction codes are POSITIONAL (FIRST_CALL_TRANSACTION +
 * declaration index), and Aster and OpenAlly are two independently-installed APKs
 * that can be updated out of step. Inserting or reordering a method silently
 * re-targets every later verb across a version skew — an old Aster would run
 * `readLargeResult` when a new OpenAlly called `executeCommand`. Add new methods at
 * the END, and keep this file byte-identical to
 * `aster-one/apps/mobile/modules/aster-ipc/android/src/main/aidl/com/aster/ipc/IAsterService.aidl`.
 */
interface IAsterService {
    String authenticate(String token);
    String executeCommand(String action, String paramsJson);
    ParcelFileDescriptor readLargeResult(String resultId);
    void registerCallback(IAsterCallback callback);
    void unregisterCallback(IAsterCallback callback);
    List<String> getAvailableTools();
    void disconnect();

    /**
     * Stream one serialized `buildGeometry` frame for the ambient companion face that
     * Aster draws over other apps on OpenAlly's behalf (OpenAlly holds no
     * draw-over-other-apps permission; it owns the face rig, Aster owns the window).
     *
     * `oneway` because this runs at frame rate and must never block OpenAlly's JS
     * thread, and NOT `executeCommand` because that path takes a `runBlocking` and
     * writes two audit rows per call.
     *
     * UTF-8 bytes, not String: a Java String parcels as UTF-16, which would DOUBLE the
     * wire size of an all-ASCII payload. Size matters here beyond bandwidth — a
     * process's `oneway` transactions are capped at half its ~1 MB binder mapping, and
     * that budget is SHARED with system_server's delivery of accessibility events into
     * Aster. Overrunning it would degrade the screen-control capability itself, so the
     * sender rate-limits and bounds its in-flight frames, and the receiver drains on a
     * binder thread and coalesces to the newest frame.
     */
    oneway void pushCompanionFrame(in byte[] frame);

    /** APPEND-ONLY low-rate companion readout lane. UTF-8 JSON; `null` clears. */
    oneway void pushCompanionStatus(in byte[] status);

    /** APPEND-ONLY native System Pulse policy. UTF-8 JSON, whitelist validated. */
    oneway void pushCompanionConfiguration(in byte[] configuration);
}
