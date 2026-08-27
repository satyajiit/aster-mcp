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
 * the END, and keep this file byte-identical to its twin in the other repo
 * (`aster-one/apps/mobile/modules/aster-ipc/android/src/main/aidl/…` /
 * `aster-mcp/apps/android/app/src/main/aidl/…`).
 *
 * Four `oneway` companion-face lanes — pushCompanionFrame / pushCompanionStatus /
 * pushCompanionConfiguration / pushCompanionState — were removed when the mobile
 * companion face was retired. That was safe ONLY because they were the LAST four
 * declarations: every surviving verb keeps the transaction code it already had, so
 * no skew combination re-targets anything. A shipped OpenAlly that still calls them
 * against a newer Aster hits an unknown transaction, which for a `oneway` call fails
 * silently — the old face simply stops painting, which is the intent. Removing a
 * method from anywhere but the end would NOT be safe.
 */
interface IAsterService {
    String authenticate(String token);
    String executeCommand(String action, String paramsJson);
    ParcelFileDescriptor readLargeResult(String resultId);
    void registerCallback(IAsterCallback callback);
    void unregisterCallback(IAsterCallback callback);
    List<String> getAvailableTools();
    void disconnect();
}
