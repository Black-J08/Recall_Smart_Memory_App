package com.smartmemory.recall.data.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.net.InetAddress
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Handles file downloads with progress tracking using OkHttp.
 */
@Singleton
class FileDownloader @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    
    private val client = OkHttpClient.Builder()
        .dns(object : okhttp3.Dns {
            override fun lookup(hostname: String): List<java.net.InetAddress> {
                // 1. Try standard lookup first
                try {
                    Log.d("FileDownloader", "DNS Lookup starting for: $hostname (Default)")
                    val results = okhttp3.Dns.SYSTEM.lookup(hostname)
                    Log.d("FileDownloader", "DNS Lookup success for $hostname (Default): $results")
                    return results
                } catch (e: Exception) {
                    Log.w("FileDownloader", "Default DNS Lookup failed for $hostname, trying all available networks...")
                }

                // 2. Fallback: Try each available network specifically
                val allNetworks = connectivityManager.allNetworks
                for (network in allNetworks) {
                    try {
                        val caps = connectivityManager.getNetworkCapabilities(network)
                        if (caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true) {
                            Log.d("FileDownloader", "Trying DNS lookup on network $network for $hostname")
                            val results = network.getAllByName(hostname).toList()
                            if (results.isNotEmpty()) {
                                Log.d("FileDownloader", "DNS Lookup success on network $network for $hostname: $results")
                                return results
                            }
                        }
                    } catch (e: Exception) {
                        Log.w("FileDownloader", "DNS lookup failed on network $network: ${e.message}")
                    }
                }

                val errorMsg = "Unable to resolve host \"$hostname\": No address associated with hostname"
                Log.e("FileDownloader", "All DNS resolution attempts failed for $hostname")
                throw java.net.UnknownHostException(errorMsg)
            }
        })
        .followRedirects(true)
        .followSslRedirects(true)
        .build()


    suspend fun downloadFile(
        url: String,
        targetFile: File,
        onProgress: (Float) -> Unit
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Recall-Android/1.0 (Mobile; Android)")
                .build()

            Log.d("FileDownloader", "Starting download from: $url")
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val error = "Download failed: HTTP ${response.code} for $url"
                    Log.e("FileDownloader", error)
                    return@withContext Result.failure(
                        Exception(error)
                    )
                }

                val body = response.body ?: return@withContext Result.failure(
                    Exception("Empty response body")
                )

                val contentLength = body.contentLength()
                val inputStream = body.byteStream()
                
                // Ensure parent directory exists
                targetFile.parentFile?.mkdirs()
                
                FileOutputStream(targetFile).use { outputStream ->
                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    var totalBytesRead = 0L

                    while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                        outputStream.write(buffer, 0, bytesRead)
                        totalBytesRead += bytesRead

                        if (contentLength > 0) {
                            val progress = totalBytesRead.toFloat() / contentLength.toFloat()
                            onProgress(progress)
                        }
                    }
                }

                onProgress(1.0f)
                Result.success(Unit)
            }
        } catch (e: java.net.UnknownHostException) {
            Log.e("FileDownloader", "DNS Resolution failed for $url", e)
            // Clean up partial download
            if (targetFile.exists()) {
                targetFile.delete()
            }
            Result.failure(Exception("Cannot resolve huggingface.co. Please check your device's internet connection and DNS settings."))
        } catch (e: Exception) {
            Log.e("FileDownloader", "Download error for $url", e)
            // Clean up partial download
            if (targetFile.exists()) {
                targetFile.delete()
            }
            Result.failure(e)
        }
    }

    /**
     * Fetches the content of a URL as a string (for JSON configs).
     */
    suspend fun fetchString(url: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Recall-Android/1.0 (Mobile; Android)")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext Result.failure(
                        Exception("Fetch failed: HTTP ${response.code}")
                    )
                }

                val body = response.body?.string() ?: return@withContext Result.failure(
                    Exception("Empty response body")
                )

                Result.success(body)
            }
        } catch (e: java.net.UnknownHostException) {
            Result.failure(Exception("Cannot resolve huggingface.co. Please check your device's internet connection and DNS settings."))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
