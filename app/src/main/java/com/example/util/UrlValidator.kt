package com.example.util

import java.util.regex.Pattern

object UrlValidator {

    // TikTok domain patterns:
    // Supports tiktok.com, vt.tiktok.com, vm.tiktok.com, m.tiktok.com, v.douyin.com, etc.
    private val TIKTOK_PATTERN = Pattern.compile(
        "^https?://([a-zA-Z0-9_-]+\\.)*(tiktok\\.com|douyin\\.com)(/.*)?$",
        Pattern.CASE_INSENSITIVE
    )

    private val URL_FINDER = Pattern.compile(
        "https?://[a-zA-Z0-9_.-]+(?:\\.[a-zA-Z]{2,})+(?:/[^\\s]*)?",
        Pattern.CASE_INSENSITIVE
    )

    fun isTikTokUrl(input: String?): Boolean {
        if (input.isNullOrBlank()) return false
        val sanitized = sanitizeUrl(input)
        val matcher = TIKTOK_PATTERN.matcher(sanitized)
        return matcher.matches()
    }

    /**
     * Extracts and cleans the first valid URL from a raw input string.
     * Works when text is shared from TikTok (e.g. "Check out this video: https://vt.tiktok.com/ZS.../").
     */
    fun sanitizeUrl(input: String): String {
        val trimmed = input.trim()
        val matcher = URL_FINDER.matcher(trimmed)
        var url = if (matcher.find()) {
            matcher.group(0) ?: trimmed
        } else {
            trimmed
        }

        // Remove trailing punctuation often captured from chat messages or markdown
        url = url.trimEnd(')', ']', '>', '}', '"', '\'', ',', ';', '.')

        // If URL doesn't start with protocol but matches a domain, prepend https://
        if (!url.startsWith("http://") && !url.startsWith("https://") && url.isNotBlank()) {
            url = "https://$url"
        }
        return url
    }
}
