package com.smartmemory.recall.util

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Uri
import android.provider.Settings
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Utility to detect and handle network restrictions on Xiaomi/MIUI devices.
 */
@Singleton
class NetworkRestrictionDetector @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    
    /**
     * Checks if the app is likely in a network-restricted state.
     * This typically happens on Xiaomi devices when "Allow network access" is disabled.
     * 
     * Detection logic: If we have available networks but no active network,
     * the OS is likely blocking our app from using them.
     */
    fun isNetworkRestricted(): Boolean {
        val activeNetwork = connectivityManager.activeNetwork
        val allNetworks = connectivityManager.allNetworks
        
        // If we have networks available but no active network, likely restricted
        return allNetworks.isNotEmpty() && activeNetwork == null
    }
    
    /**
     * Opens the App Info settings page where users can enable network access.
     * On Xiaomi devices, this takes them to the page with "Mobile data usage" settings.
     */
    fun openNetworkSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }
}
