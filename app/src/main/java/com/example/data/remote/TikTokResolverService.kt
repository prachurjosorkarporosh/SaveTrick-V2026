package com.example.data.remote

import android.util.Log
import com.example.data.model.MediaResult
import com.example.data.model.MediaType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class TikTokResolverService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    suspend fun resolveTikTokUrl(tiktokUrl: String): Result<MediaResult> = withContext(Dispatchers.IO) {
        try {
            // First primary resolver: TikWM API
            val tikwmResult = tryResolveTikWm(tiktokUrl)
            if (tikwmResult.isSuccess) {
                return@withContext tikwmResult
            }

            // Fallback resolver: TiklyDown / Snaptik fallback
            val fallbackResult = tryResolveFallback(tiktokUrl)
            if (fallbackResult.isSuccess) {
                return@withContext fallbackResult
            }

            return@withContext Result.failure(tikwmResult.exceptionOrNull() ?: Exception("Unable to parse TikTok media"))
        } catch (e: Exception) {
            Log.e("TikTokResolver", "Resolution error", e)
            Result.failure(e)
        }
    }

    private fun tryResolveTikWm(url: String): Result<MediaResult> {
        return try {
            val requestBody = FormBody.Builder()
                .add("url", url)
                .add("count", "12")
                .add("cursor", "0")
                .add("web", "1")
                .add("hd", "1")
                .build()

            val request = Request.Builder()
                .url("https://www.tikwm.com/api/")
                .post(requestBody)
                .addHeader("User-Agent", "Mozilla/5.0 (Linux; Android 14; SaveTrick/2.5.7) AppleWebKit/537.36")
                .build()

            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: return Result.failure(Exception("Empty response"))
            val json = JSONObject(body)

            val code = json.optInt("code", -1)
            if (code != 0) {
                val msg = json.optString("msg", "Unknown error")
                return Result.failure(Exception(msg))
            }

            val data = json.getJSONObject("data")
            val title = data.optString("title", "TikTok Media")
            val cover = data.optString("cover", "")
            val play = data.optString("play", "")
            val music = data.optString("music", "")
            val duration = data.optLong("duration", 0L)

            val authorObj = data.optJSONObject("author")
            val author = authorObj?.optString("unique_id") ?: authorObj?.optString("nickname")

            // Check if slideshow
            val imagesArray = data.optJSONArray("images")
            val images = mutableListOf<String>()
            if (imagesArray != null && imagesArray.length() > 0) {
                for (i in 0 until imagesArray.length()) {
                    val imgUrl = imagesArray.optString(i, "")
                    if (imgUrl.isNotBlank()) {
                        images.add(imgUrl)
                    }
                }
            }

            if (images.isNotEmpty()) {
                // PHOTO_SLIDESHOW
                Result.success(
                    MediaResult(
                        type = MediaType.PHOTO_SLIDESHOW,
                        title = title.ifBlank { "TikTok Slideshow" },
                        author = author,
                        coverUrl = cover.ifBlank { images.firstOrNull() },
                        videoUrl = null,
                        audioUrl = music.ifBlank { null },
                        images = images,
                        durationSeconds = duration,
                        sourceUrl = url
                    )
                )
            } else if (play.isNotBlank()) {
                // VIDEO
                val fullPlayUrl = if (play.startsWith("http")) play else "https://www.tikwm.com$play"
                val fullAudioUrl = if (music.isNotBlank()) {
                    if (music.startsWith("http")) music else "https://www.tikwm.com$music"
                } else null

                Result.success(
                    MediaResult(
                        type = MediaType.VIDEO,
                        title = title.ifBlank { "TikTok Video" },
                        author = author,
                        coverUrl = cover,
                        videoUrl = fullPlayUrl,
                        audioUrl = fullAudioUrl,
                        images = emptyList(),
                        durationSeconds = duration,
                        sourceUrl = url
                    )
                )
            } else {
                Result.failure(Exception("No playable video or images found in response"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun tryResolveFallback(url: String): Result<MediaResult> {
        return try {
            val request = Request.Builder()
                .url("https://api.tiklydown.eu.org/api/download?url=$url")
                .get()
                .addHeader("User-Agent", "Mozilla/5.0 (Linux; Android 14; SaveTrick/2.5.7) AppleWebKit/537.36")
                .build()

            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: return Result.failure(Exception("Empty fallback response"))
            val json = JSONObject(body)

            val title = json.optString("title", "TikTok Media")
            val videoObj = json.optJSONObject("video")
            val musicObj = json.optJSONObject("music")
            val imagesArr = json.optJSONArray("images")

            val images = mutableListOf<String>()
            if (imagesArr != null) {
                for (i in 0 until imagesArr.length()) {
                    val obj = imagesArr.optJSONObject(i)
                    val urlStr = obj?.optString("url") ?: imagesArr.optString(i)
                    if (urlStr.isNotBlank()) images.add(urlStr)
                }
            }

            val videoUrl = videoObj?.optString("noWatermark") ?: videoObj?.optString("watermark")
            val audioUrl = musicObj?.optString("play_url")

            if (images.isNotEmpty()) {
                Result.success(
                    MediaResult(
                        type = MediaType.PHOTO_SLIDESHOW,
                        title = title,
                        author = json.optJSONObject("author")?.optString("name"),
                        coverUrl = images.firstOrNull(),
                        videoUrl = null,
                        audioUrl = audioUrl,
                        images = images,
                        sourceUrl = url
                    )
                )
            } else if (!videoUrl.isNullOrBlank()) {
                Result.success(
                    MediaResult(
                        type = MediaType.VIDEO,
                        title = title,
                        author = json.optJSONObject("author")?.optString("name"),
                        coverUrl = videoObj.optString("cover"),
                        videoUrl = videoUrl,
                        audioUrl = audioUrl,
                        images = emptyList(),
                        sourceUrl = url
                    )
                )
            } else {
                Result.failure(Exception("Fallback resolver failed to locate video/images"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
