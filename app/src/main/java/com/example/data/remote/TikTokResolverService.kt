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
        .connectTimeout(7, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    private val fastMemoryCache = java.util.concurrent.ConcurrentHashMap<String, MediaResult>()

    suspend fun resolveTikTokUrl(tiktokUrl: String): Result<MediaResult> = withContext(Dispatchers.IO) {
        fastMemoryCache[tiktokUrl]?.let {
            return@withContext Result.success(it)
        }
        try {
            // First primary resolver: TikWM API (HD video, slideshows, audios)
            val tikwmResult = tryResolveTikWm(tiktokUrl)
            if (tikwmResult.isSuccess) {
                tikwmResult.getOrNull()?.let { fastMemoryCache[tiktokUrl] = it }
                return@withContext tikwmResult
            }

            // Second fallback resolver: TiklyDown
            val fallbackResult = tryResolveFallback(tiktokUrl)
            if (fallbackResult.isSuccess) {
                fallbackResult.getOrNull()?.let { fastMemoryCache[tiktokUrl] = it }
                return@withContext fallbackResult
            }

            // Third fallback: Lovetik API
            val lovetikResult = tryResolveLovetik(tiktokUrl)
            if (lovetikResult.isSuccess) {
                lovetikResult.getOrNull()?.let { fastMemoryCache[tiktokUrl] = it }
                return@withContext lovetikResult
            }

            return@withContext Result.failure(
                tikwmResult.exceptionOrNull()
                    ?: fallbackResult.exceptionOrNull()
                    ?: Exception("Unable to parse TikTok media")
            )
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
            val rawCover = data.optString("cover", "")
            val cover = if (rawCover.isNotBlank()) {
                if (rawCover.startsWith("http")) rawCover else "https://www.tikwm.com$rawCover"
            } else ""
            val rawPlay = data.optString("play", "").ifBlank {
                data.optString("hdplay", "").ifBlank {
                    data.optString("wmplay", "")
                }
            }
            val play = rawPlay
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
                        val fullImg = if (imgUrl.startsWith("http")) imgUrl else "https://www.tikwm.com$imgUrl"
                        images.add(fullImg)
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
            val coverUrl = videoObj?.optString("cover") ?: ""

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
                        coverUrl = coverUrl,
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

    private fun tryResolveLovetik(url: String): Result<MediaResult> {
        return try {
            val form = FormBody.Builder()
                .add("query", url)
                .build()
            val request = Request.Builder()
                .url("https://lovetik.com/api/ajax/search")
                .post(form)
                .addHeader("User-Agent", "Mozilla/5.0 (Linux; Android 14; SaveTrick/2.5.7) AppleWebKit/537.36")
                .build()

            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: return Result.failure(Exception("Empty response"))
            val json = JSONObject(body)

            val desc = json.optString("desc", "TikTok Video")
            val author = json.optString("author", "")
            val cover = json.optString("cover", "")
            val links = json.optJSONArray("links")

            var videoUrl: String? = null
            var audioUrl: String? = null

            if (links != null) {
                for (i in 0 until links.length()) {
                    val linkObj = links.optJSONObject(i) ?: continue
                    val t = linkObj.optString("t")
                    val a = linkObj.optString("a")
                    if (videoUrl == null && (t.contains("MP4", ignoreCase = true) || t.contains("HD", ignoreCase = true))) {
                        videoUrl = a
                    }
                    if (audioUrl == null && t.contains("MP3", ignoreCase = true)) {
                        audioUrl = a
                    }
                }
            }

            if (!videoUrl.isNullOrBlank()) {
                Result.success(
                    MediaResult(
                        type = MediaType.VIDEO,
                        title = desc,
                        author = author,
                        coverUrl = cover,
                        videoUrl = videoUrl,
                        audioUrl = audioUrl,
                        images = emptyList(),
                        sourceUrl = url
                    )
                )
            } else {
                Result.failure(Exception("Lovetik resolver found no downloadable link"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
