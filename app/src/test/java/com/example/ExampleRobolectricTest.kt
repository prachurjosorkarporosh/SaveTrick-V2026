package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.util.UrlValidator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("SaveTrick", appName)
    }

    @Test
    fun `validate tiktok url detection`() {
        // Valid TikTok URLs
        assertTrue(UrlValidator.isTikTokUrl("https://www.tiktok.com/@user/video/7123456789012345678"))
        assertTrue(UrlValidator.isTikTokUrl("https://vt.tiktok.com/ZS123456/"))
        assertTrue(UrlValidator.isTikTokUrl("https://vm.tiktok.com/ZM123456/"))
        assertTrue(UrlValidator.isTikTokUrl("https://m.tiktok.com/v/7123456789.html"))
        assertTrue(UrlValidator.isTikTokUrl("https://www.tiktok.com/@user/photo/7123456789012345678"))

        // Invalid non-TikTok URLs
        assertFalse(UrlValidator.isTikTokUrl("https://www.youtube.com/watch?v=dQw4w9WgXcQ"))
        assertFalse(UrlValidator.isTikTokUrl("https://www.instagram.com/reel/C123456/"))
        assertFalse(UrlValidator.isTikTokUrl("https://www.facebook.com/watch/?v=123456"))
        assertFalse(UrlValidator.isTikTokUrl("not_a_valid_url"))
        assertFalse(UrlValidator.isTikTokUrl(""))
    }

    @Test
    fun `verify bengali locale string resolution`() {
        val baseContext = ApplicationProvider.getApplicationContext<Context>()
        val bnContext = com.example.util.LocaleHelper.applyLocale(baseContext, "bn")
        val downloadingText = bnContext.getString(R.string.tab_downloading)
        assertEquals("ডাউনলোড হচ্ছে", downloadingText)
    }
}

