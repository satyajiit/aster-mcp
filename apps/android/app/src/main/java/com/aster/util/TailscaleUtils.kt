package com.aster.util

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.util.Log
import java.net.NetworkInterface

/**
 * Utility class for Tailscale integration.
 * Checks if Tailscale is installed and VPN is active.
 */
object TailscaleUtils {

    private const val TAG = "TailscaleUtils"
    private const val TAILSCALE_PACKAGE = "com.tailscale.ipn"
    private const val TAILSCALE_INTERFACE_PREFIX = "tailscale"
    private const val TAILSCALE_TUN_PREFIX = "tun" // Tailscale uses tun interface

    /**
     * Check if Tailscale app is installed on the device.
     */
    fun isInstalled(context: Context): Boolean {
        return try {
            context.packageManager.getPackageInfo(TAILSCALE_PACKAGE, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    /**
     * Check if Tailscale VPN is currently active.
     * This checks for the presence of a Tailscale network interface.
     */
    fun isVpnActive(context: Context): Boolean {
        // Method 1: Check for VPN transport in active network
        if (isVpnTransportActive(context)) {
            return true
        }

        // Method 2: Check for Tailscale network interface
        return hasTailscaleInterface()
    }

    /**
     * Check if any VPN transport is active using ConnectivityManager.
     */
    private fun isVpnTransportActive(context: Context): Boolean {
        return try {
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val activeNetwork = connectivityManager.activeNetwork ?: return false
            val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork) ?: return false
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN)
        } catch (e: Exception) {
            Log.w(TAG, "Error checking VPN transport: ${e.message}")
            false
        }
    }

    /**
     * Check if a Tailscale network interface exists.
     * Tailscale typically creates a tun0 interface or tailscale0.
     */
    private fun hasTailscaleInterface(): Boolean {
        return try {
            val interfaces = NetworkInterface.getNetworkInterfaces()?.toList() ?: emptyList()
            interfaces.any { iface ->
                val name = iface.name.lowercase()
                // Tailscale creates interfaces like "tailscale0" or uses "tun0"
                (name.startsWith(TAILSCALE_INTERFACE_PREFIX) ||
                 (name.startsWith(TAILSCALE_TUN_PREFIX) && iface.isUp && !iface.isLoopback))
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error checking network interfaces: ${e.message}")
            false
        }
    }

    /**
     * Get the Tailscale IP address if available.
     * Tailscale IPs are in the 100.x.x.x range.
     */
    fun getTailscaleIp(): String? {
        return try {
            val interfaces = NetworkInterface.getNetworkInterfaces()?.toList() ?: emptyList()
            for (iface in interfaces) {
                if (!iface.isUp || iface.isLoopback) continue

                val name = iface.name.lowercase()
                if (name.startsWith(TAILSCALE_INTERFACE_PREFIX) || name.startsWith(TAILSCALE_TUN_PREFIX)) {
                    val addresses = iface.inetAddresses.toList()
                    for (addr in addresses) {
                        val ip = addr.hostAddress ?: continue
                        // Tailscale uses 100.x.x.x CGNAT range
                        if (ip.startsWith("100.")) {
                            return ip
                        }
                    }
                }
            }
            null
        } catch (e: Exception) {
            Log.w(TAG, "Error getting Tailscale IP: ${e.message}")
            null
        }
    }

    /**
     * Get the launch intent for Tailscale app.
     */
    fun getLaunchIntent(context: Context): Intent? {
        return context.packageManager.getLaunchIntentForPackage(TAILSCALE_PACKAGE)?.apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    /**
     * Launch the Tailscale app.
     * Returns true if launched successfully.
     */
    fun launchTailscale(context: Context): Boolean {
        val intent = getLaunchIntent(context)
        return if (intent != null) {
            try {
                context.startActivity(intent)
                true
            } catch (e: Exception) {
                Log.e(TAG, "Failed to launch Tailscale: ${e.message}")
                false
            }
        } else {
            Log.w(TAG, "Tailscale launch intent not found")
            false
        }
    }

    /**
     * Open Play Store to install Tailscale.
     */
    fun openPlayStore(context: Context) {
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = android.net.Uri.parse("market://details?id=$TAILSCALE_PACKAGE")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            // Fallback to browser
            try {
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    data = android.net.Uri.parse("https://play.google.com/store/apps/details?id=$TAILSCALE_PACKAGE")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            } catch (e2: Exception) {
                Log.e(TAG, "Failed to open Play Store: ${e2.message}")
            }
        }
    }

    /**
     * Complete status check result.
     */
    data class TailscaleStatus(
        val isInstalled: Boolean,
        val isVpnActive: Boolean,
        val tailscaleIp: String?
    ) {
        val isReady: Boolean get() = isInstalled && isVpnActive
    }

    /**
     * Get complete Tailscale status.
     */
    fun getStatus(context: Context): TailscaleStatus {
        val installed = isInstalled(context)
        val vpnActive = if (installed) isVpnActive(context) else false
        val ip = if (vpnActive) getTailscaleIp() else null

        return TailscaleStatus(
            isInstalled = installed,
            isVpnActive = vpnActive,
            tailscaleIp = ip
        )
    }
}
