package com.aster.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

data class ServerConfig(
    val host: String = "",
    val port: Int = 5987,
    val autoConnect: Boolean = false
)

class SettingsDataStore @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    private object Keys {
        val SERVER_HOST = stringPreferencesKey("server_host")
        val SERVER_PORT = intPreferencesKey("server_port")
        val SERVER_URL = stringPreferencesKey("server_url")
        val AUTO_CONNECT = booleanPreferencesKey("auto_connect")
        val AUTO_START_ON_BOOT = booleanPreferencesKey("auto_start_on_boot")
        val DEVICE_ID = stringPreferencesKey("device_id")

        // New keys for revamp
        val ONBOARDING_COMPLETE = booleanPreferencesKey("onboarding_complete")
        val LAST_MODE = stringPreferencesKey("last_mode")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val IPC_TOKEN = stringPreferencesKey("ipc_token")
        val MCP_PORT = intPreferencesKey("mcp_port")
        val AUTO_START_MODE = stringPreferencesKey("auto_start_mode")
    }

    val serverConfig: Flow<ServerConfig> = dataStore.data.map { prefs ->
        ServerConfig(
            host = prefs[Keys.SERVER_HOST] ?: "",
            port = prefs[Keys.SERVER_PORT] ?: 5987,
            autoConnect = prefs[Keys.AUTO_CONNECT] ?: false
        )
    }

    val serverUrl: Flow<String?> = dataStore.data.map { prefs ->
        prefs[Keys.SERVER_URL]
    }

    val autoStartOnBoot: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[Keys.AUTO_START_ON_BOOT] ?: false
    }

    val deviceId: Flow<String?> = dataStore.data.map { prefs ->
        prefs[Keys.DEVICE_ID]
    }

    val onboardingComplete: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[Keys.ONBOARDING_COMPLETE] ?: false
    }

    val lastMode: Flow<String?> = dataStore.data.map { prefs ->
        prefs[Keys.LAST_MODE]
    }

    val themeMode: Flow<String> = dataStore.data.map { prefs ->
        prefs[Keys.THEME_MODE] ?: "system"
    }

    val ipcToken: Flow<String?> = dataStore.data.map { prefs ->
        prefs[Keys.IPC_TOKEN]
    }

    val mcpPort: Flow<Int> = dataStore.data.map { prefs ->
        prefs[Keys.MCP_PORT] ?: 8080
    }

    val autoStartMode: Flow<String?> = dataStore.data.map { prefs ->
        prefs[Keys.AUTO_START_MODE]
    }

    suspend fun saveServerUrl(url: String) {
        dataStore.edit { prefs ->
            prefs[Keys.SERVER_URL] = url
        }
    }

    suspend fun saveServerConfig(host: String, port: Int, autoConnect: Boolean = false) {
        dataStore.edit { prefs ->
            prefs[Keys.SERVER_HOST] = host
            prefs[Keys.SERVER_PORT] = port
            prefs[Keys.AUTO_CONNECT] = autoConnect
        }
    }

    suspend fun setAutoStartOnBoot(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[Keys.AUTO_START_ON_BOOT] = enabled
        }
    }

    suspend fun saveDeviceId(deviceId: String) {
        dataStore.edit { prefs ->
            prefs[Keys.DEVICE_ID] = deviceId
        }
    }

    suspend fun clearDeviceId() {
        dataStore.edit { prefs ->
            prefs.remove(Keys.DEVICE_ID)
        }
    }

    suspend fun setOnboardingComplete(complete: Boolean) {
        dataStore.edit { prefs ->
            prefs[Keys.ONBOARDING_COMPLETE] = complete
        }
    }

    suspend fun setLastMode(mode: String) {
        dataStore.edit { prefs ->
            prefs[Keys.LAST_MODE] = mode
        }
    }

    suspend fun setThemeMode(mode: String) {
        dataStore.edit { prefs ->
            prefs[Keys.THEME_MODE] = mode
        }
    }

    suspend fun saveIpcToken(token: String) {
        dataStore.edit { prefs ->
            prefs[Keys.IPC_TOKEN] = token
        }
    }

    suspend fun setMcpPort(port: Int) {
        dataStore.edit { prefs ->
            prefs[Keys.MCP_PORT] = port
        }
    }

    suspend fun setAutoStartMode(mode: String?) {
        dataStore.edit { prefs ->
            if (mode != null) {
                prefs[Keys.AUTO_START_MODE] = mode
            } else {
                prefs.remove(Keys.AUTO_START_MODE)
            }
        }
    }
}
