package com.dangle.jobtracker.util

import android.util.Log
import java.net.URI

object UrlUtils {
    private const val TAG = "UrlUtils"

    /**
     * Extracts the host domain from a given URL string.
     * e.g., "https://google.com/jobs" -> "google.com"
     */
    fun extractDomain(url: String): String? {
        if (url.isBlank()) return null
        
        return try {
            // Ensure the URL has a scheme for URI to parse it correctly
            val formattedUrl = if (!url.contains("://")) "https://$url" else url
            val uri = URI(formattedUrl)
            val host = uri.host ?: ""
            val domain = if (host.startsWith("www.")) host.substring(4) else host
            Log.d(TAG, "Extracted domain '$domain' from original url '$url'")
            domain
        } catch (e: Exception) {
            Log.e(TAG, "Failed to extract domain from '$url'", e)
            null
        }
    }

    /**
     * Returns a URL for the company's favicon/logo using Google's favicon service.
     */
    fun getLogoUrl(jobUrl: String): String? {
        val domain = extractDomain(jobUrl)
        return if (domain != null && domain.isNotBlank()) {
            val logoUrl = "https://www.google.com/s2/favicons?domain=$domain&sz=128"
            Log.d(TAG, "Generated logo URL: $logoUrl")
            logoUrl
        } else {
            Log.w(TAG, "Could not generate logo URL for jobUrl: '$jobUrl'")
            null
        }
    }
}
