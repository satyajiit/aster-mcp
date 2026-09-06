package com.aster.service.execution

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File

/** ABI declaration fixture only. It does not replace either APK's owner-run build. */
class ExecutionAidlContractTest {
    private val relative = "apps/android/app/src/main/aidl/com/aster/ipc/IAsterService.aidl"
    private fun ancestors() = generateSequence(File(System.getProperty("user.dir"))) { it.parentFile }
    private fun companion(): File = ancestors().flatMap {
        sequenceOf(File(it, relative), File(it, "aster-mcp/$relative"), File(it, "app/src/main/aidl/com/aster/ipc/IAsterService.aidl"))
    }.first { it.isFile }

    @Test fun originalTransactionPositionsStayUnchangedAndV2OnlyAppends() {
        val source = companion().readText().replace(Regex("/\\*.*?\\*/", RegexOption.DOT_MATCHES_ALL), "")
        val names = Regex("(?:String|void|ParcelFileDescriptor|List<String>)\\s+(\\w+)\\s*\\(")
            .findAll(source).map { it.groupValues[1] }.toList()
        assertEquals(listOf("authenticate", "executeCommand", "readLargeResult", "registerCallback", "unregisterCallback",
            "getAvailableTools", "disconnect", "pushCompanionFrame", "pushCompanionStatus", "pushCompanionConfiguration", "pushCompanionState"), names.take(11))
        assertEquals(listOf("getExecutionCapabilities", "executeTracked", "getExecutionStatus", "sealExecutionIfUnstarted", "executeTrackedFromPipe"), names.drop(11))
        assertTrue(source.contains("String executeTrackedFromPipe(in ParcelFileDescriptor request);"))
    }

    @Test fun independentlyInstalledOpenAllyAndCompanionUseIdenticalDeclarations() {
        val mobile = ancestors().map {
            File(it, "aster-one/apps/mobile/modules/aster-ipc/android/src/main/aidl/com/aster/ipc/IAsterService.aidl")
        }.firstOrNull { it.isFile }
        // Standalone companion checkouts do not contain the sibling repository.
        assumeTrue(mobile != null)
        assertEquals(companion().readText(), mobile!!.readText())
    }
}
