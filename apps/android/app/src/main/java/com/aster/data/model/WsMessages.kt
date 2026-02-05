package com.aster.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/**
 * WebSocket message types matching the MCP server protocol.
 */

// Client -> Server messages

@Serializable
data class AuthMessage(
    val type: String = "auth",
    val deviceId: String,
    val deviceName: String,
    val model: String,
    val manufacturer: String,
    val platform: String = "android",
    val osVersion: String,
    val appVersion: String
)

@Serializable
data class CommandResponse(
    val type: String = "command_response",
    val id: String,
    val success: Boolean,
    val data: JsonElement? = null,
    val error: String? = null
)

@Serializable
data class EventMessage(
    val type: String = "event",
    val eventType: String,
    val data: Map<String, JsonElement>,
    val timestamp: Long = System.currentTimeMillis()
)

@Serializable
data class HeartbeatMessage(
    val type: String = "heartbeat",
    val timestamp: Long = System.currentTimeMillis()
)

// Server -> Client messages

@Serializable
data class AuthResult(
    val type: String,
    val success: Boolean,
    val status: String, // "pending", "approved", "rejected"
    val message: String? = null
)

@Serializable
data class Command(
    val type: String,
    val id: String,
    val action: String,
    val params: Map<String, JsonElement>? = null
)

@Serializable
data class HeartbeatAck(
    val type: String,
    val timestamp: Long
)

// Generic incoming message for parsing
@Serializable
data class IncomingMessage(
    val type: String
)

// Connection state
enum class ConnectionState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    PENDING_APPROVAL,
    APPROVED,
    REJECTED,
    ERROR
}

// Device status
enum class DeviceStatus(val value: String) {
    PENDING("pending"),
    APPROVED("approved"),
    REJECTED("rejected");

    companion object {
        fun fromValue(value: String): DeviceStatus {
            return entries.find { it.value == value } ?: PENDING
        }
    }
}
