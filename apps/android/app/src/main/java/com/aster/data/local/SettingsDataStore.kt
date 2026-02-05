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
}
