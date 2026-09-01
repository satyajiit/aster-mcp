package com.aster.data.websocket

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.UnknownServiceException
import javax.net.ssl.SSLException

class ConnectionFailureMapperTest {

    @Test
    fun tlsMessage_matchesPlannedUserString() {
        assertEquals(
            "Server at 100.64.0.1 doesn't speak TLS on this port. " +
                "Use `ws://` on the LAN, or the `wss://<magicdns>` URL printed by `aster status` when Tailscale is active.",
            ConnectionFailureMapper.tlsMessage("100.64.0.1")
        )
    }

    @Test
    fun cleartextMessage_matchesPlannedUserString() {
        assertEquals(
            "This build blocks cleartext — update the Aster app.",
            ConnectionFailureMapper.CLEARTEXT_MESSAGE
        )
    }

    @Test
    fun map_tlsHandshakeUsesTlsMessage() {
        val mapped = ConnectionFailureMapper.map(
            SSLException("Unable to parse TLS packet header"),
            "example.ts.net"
        )
        assertEquals(ConnectionFailureMapper.tlsMessage("example.ts.net"), mapped)
    }

    @Test
    fun map_cleartextPolicyUsesCleartextMessage() {
        val mapped = ConnectionFailureMapper.map(
            UnknownServiceException("CLEARTEXT communication not permitted by network security policy"),
            "192.168.1.10"
        )
        assertEquals(ConnectionFailureMapper.CLEARTEXT_MESSAGE, mapped)
    }

    @Test
    fun isDiagnostic_trueForTlsAndCleartext() {
        assertTrue(ConnectionFailureMapper.isDiagnostic(SSLException("handshake")))
        assertTrue(
            ConnectionFailureMapper.isDiagnostic(
                UnknownServiceException("CLEARTEXT communication not permitted")
            )
        )
        assertFalse(ConnectionFailureMapper.isDiagnostic(IllegalStateException("boom")))
    }
}

class WsUrlPolicyTest {

    @Test
    fun buildWsUrl_bareLanAndTailscaleDefaultToWs() {
        assertEquals("ws://192.168.1.10:5987", WsUrlPolicy.buildWsUrl("192.168.1.10:5987"))
        assertEquals("ws://10.0.0.2:5987", WsUrlPolicy.buildWsUrl("10.0.0.2:5987"))
        assertEquals("ws://100.64.1.2:5987", WsUrlPolicy.buildWsUrl("100.64.1.2:5987"))
    }

    @Test
    fun buildWsUrl_keepsExplicitSchemes() {
        assertEquals("wss://box.ts.net", WsUrlPolicy.buildWsUrl("wss://box.ts.net"))
        assertEquals("ws://192.168.1.10:5987", WsUrlPolicy.buildWsUrl("ws://192.168.1.10:5987"))
        assertEquals("wss://example.com", WsUrlPolicy.buildWsUrl("https://example.com"))
        assertEquals("ws://192.168.1.10", WsUrlPolicy.buildWsUrl("http://192.168.1.10"))
    }

    @Test
    fun buildWsUrl_nonLanBareHostDefaultsToWss() {
        assertEquals("wss://example.com", WsUrlPolicy.buildWsUrl("example.com"))
    }
}
