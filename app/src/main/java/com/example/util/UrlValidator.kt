package com.example.util

import java.util.regex.Pattern

object UrlValidator {

    // TikTok domain patterns
    // tiktok.com, vt.tiktok.com, vm.tiktok.com, m.tiktok.com, v.douyin.com
    private val TIKTOK_PATTERN = Pattern.compile(
        "^(https?://)?([a-zA-Z0-9_-]+\\.)*(tiktok\\.com|douyin\\.com)(/.*)?$",
        Pattern.CASE_INSENSITIVE
    )

    fun isTikTokUrl(input: String?): Boolean {
        if (input.isNullOrBlank()) return false
        val trimmed = input.trim()
        val matcher = TIKTOK_PATTERN.matcher(trimmed)
        return matcher.matches()
    }

    fun sanitizeUrl(input: String): String {
        var url = input.trim()
        // If user pasted text containing URL, extract the http part
        val httpIndex = url.indexOf("http://")
        val httpsIndex = url.indexOf("https://")
        val startIndex = when {
            httpsIndex != -1 -> httpsIndex
            httpIndex != -1 -> httpIndex
            else -> -1
        }
        if (startIndex != -1) {
            val sub = url.substring(startIndex)
            val spaceIndex = sub.indexOfAny(charArrayOf(' ', '\n', '\t'))
            url = if (spaceIndex != -1) sub.substring(0, spaceIndex) else sub
        }
        return url
    }
}
