package com.veloris.app.data

import com.veloris.app.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

object YouTubeApi {
    private const val B = "https://www.googleapis.com/youtube/v3"
    val enabled: Boolean get() = BuildConfig.YT_API_KEY.isNotBlank()

    /** Tren musik (kategori 10). Biaya kuota: 1 unit. */
    suspend fun trending(region: String = "ID"): List<Track> = withContext(Dispatchers.IO) {
        if (!enabled) return@withContext emptyList()
        parse(Http.get("$B/videos?part=snippet&chart=mostPopular&videoCategoryId=10&maxResults=20&regionCode=$region&key=${BuildConfig.YT_API_KEY}"))
    }

    /** Pencarian. Biaya kuota: 100 unit per panggilan. */
    suspend fun search(q: String): List<Track> = withContext(Dispatchers.IO) {
        if (!enabled || q.isBlank()) return@withContext emptyList()
        parse(Http.get("$B/search?part=snippet&type=video&videoCategoryId=10&maxResults=20&q=${Http.enc(q)}&key=${BuildConfig.YT_API_KEY}"))
    }

    private fun parse(json: String): List<Track> =
        JSONObject(json).optJSONArray("items")?.mapObjects<Track> { i ->
            val idAny = i.opt("id")
            val id = if (idAny is JSONObject) idAny.optString("videoId") else idAny?.toString().orEmpty()
            val s = i.optJSONObject("snippet") ?: return@mapObjects null
            if (id.isBlank()) null
            else Track(id, s.optString("title"), s.optString("channelTitle").removeSuffix(" - Topic"), thumb(s))
        }.orEmpty()

    fun thumb(s: JSONObject): String? = s.optJSONObject("thumbnails")?.let { t ->
        listOf("high", "medium", "default").firstNotNullOfOrNull { k -> t.optJSONObject(k)?.optString("url")?.takeIf { it.isNotBlank() } }
    }
}
