package com.veloris.app.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/** Sinkron profil, playlist, dan lagu disukai dari YouTube + Spotify. Hasil di-cache agar tampil instan. */
object LibraryRepository {
    val data = MutableStateFlow(LibraryData())
    val syncing = MutableStateFlow(false)
    private lateinit var prefs: SharedPreferences

    fun init(c: Context) {
        if (::prefs.isInitialized) return
        prefs = c.applicationContext.getSharedPreferences("veloris_library", Context.MODE_PRIVATE)
        prefs.getString("json", null)?.let { runCatching { data.value = fromJson(JSONObject(it)) } }
    }

    suspend fun sync() {
        if (syncing.value) return
        syncing.value = true
        try {
            val parts = Provider.values().mapNotNull { p ->
                val t = AuthManager.token(p) ?: return@mapNotNull null
                runCatching { withContext(Dispatchers.IO) { if (p == Provider.SPOTIFY) spotify(t) else youtube(t) } }.getOrNull()
            }
            if (parts.isEmpty()) return
            val fresh = parts.flatMap { it.profiles.map { p -> p.source } }.toSet()
            val linkedIds = AuthManager.linked.value.map { it.id }.toSet()
            val ok: (String) -> Boolean = { s -> s !in fresh && s in linkedIds }
            val d = data.value
            val merged = LibraryData(
                d.profiles.filter { ok(it.source) } + parts.flatMap { it.profiles },
                d.liked.filter { ok(it.source) } + parts.flatMap { it.liked },
                d.playlists.filter { ok(it.source) } + parts.flatMap { it.playlists },
                System.currentTimeMillis(),
            )
            update(merged)
        } finally {
            syncing.value = false
        }
    }

    fun removeSource(source: String) {
        val d = data.value
        update(LibraryData(d.profiles.filter { it.source != source }, d.liked.filter { it.source != source },
            d.playlists.filter { it.source != source }, d.syncedAt))
    }

    private fun update(d: LibraryData) {
        data.value = d
        if (::prefs.isInitialized) prefs.edit().putString("json", toJson(d).toString()).apply()
    }

    private fun youtube(t: String): LibraryData {
        val b = "https://www.googleapis.com/youtube/v3"
        val ch = runCatching { JSONObject(Http.get("$b/channels?part=snippet&mine=true", t)).optJSONArray("items")?.optJSONObject(0)?.optJSONObject("snippet") }.getOrNull()
        val prof = Profile(ch?.optString("title")?.takeIf { it.isNotBlank() } ?: "YouTube", ch?.let { YouTubeApi.thumb(it) }, "youtube")
        val liked = runCatching {
            JSONObject(Http.get("$b/playlistItems?part=snippet&playlistId=LL&maxResults=50", t)).optJSONArray("items")?.mapObjects<Track> { i ->
                val s = i.optJSONObject("snippet") ?: return@mapObjects null
                val id = s.optJSONObject("resourceId")?.optString("videoId").orEmpty()
                if (id.isBlank()) null else Track(id, s.optString("title"), s.optString("videoOwnerChannelTitle").removeSuffix(" - Topic"), YouTubeApi.thumb(s))
            }.orEmpty()
        }.getOrDefault(emptyList())
        val pls = runCatching {
            JSONObject(Http.get("$b/playlists?part=snippet,contentDetails&mine=true&maxResults=50", t)).optJSONArray("items")?.mapObjects<PlaylistItem> { i ->
                val s = i.optJSONObject("snippet") ?: return@mapObjects null
                PlaylistItem(i.optString("id"), s.optString("title"), i.optJSONObject("contentDetails")?.optInt("itemCount") ?: 0, YouTubeApi.thumb(s), "youtube")
            }.orEmpty()
        }.getOrDefault(emptyList())
        return LibraryData(listOf(prof), liked, pls)
    }

    private fun spotify(t: String): LibraryData {
        val b = "https://api.spotify.com/v1"
        val me = JSONObject(Http.get("$b/me", t))
        val prof = Profile(me.optString("display_name").ifBlank { "Spotify" }, me.optJSONArray("images")?.optJSONObject(0)?.optString("url")?.takeIf { it.isNotBlank() }, "spotify")
        val liked = runCatching {
            JSONObject(Http.get("$b/me/tracks?limit=50", t)).optJSONArray("items")?.mapObjects<Track> { i ->
                val tr = i.optJSONObject("track") ?: return@mapObjects null
                Track(tr.optString("id"), tr.optString("name"), tr.optJSONArray("artists")?.optJSONObject(0)?.optString("name").orEmpty(),
                    tr.optJSONObject("album")?.optJSONArray("images")?.optJSONObject(0)?.optString("url"), "spotify")
            }.orEmpty()
        }.getOrDefault(emptyList())
        val pls = runCatching {
            JSONObject(Http.get("$b/me/playlists?limit=50", t)).optJSONArray("items")?.mapObjects<PlaylistItem> { i ->
                PlaylistItem(i.optString("id"), i.optString("name"), i.optJSONObject("tracks")?.optInt("total") ?: 0,
                    i.optJSONArray("images")?.optJSONObject(0)?.optString("url")?.takeIf { it.isNotBlank() }, "spotify")
            }.orEmpty()
        }.getOrDefault(emptyList())
        return LibraryData(listOf(prof), liked, pls)
    }

    private fun toJson(d: LibraryData) = JSONObject().apply {
        put("t", d.syncedAt)
        put("p", JSONArray(d.profiles.map { JSONObject().put("n", it.name).put("a", it.avatar ?: "").put("s", it.source) }))
        put("l", JSONArray(d.liked.map { JSONObject().put("i", it.id).put("t", it.title).put("r", it.artist).put("h", it.thumb ?: "").put("s", it.source) }))
        put("y", JSONArray(d.playlists.map { JSONObject().put("i", it.id).put("t", it.title).put("c", it.count).put("h", it.thumb ?: "").put("s", it.source) }))
    }

    private fun fromJson(j: JSONObject) = LibraryData(
        j.optJSONArray("p")?.mapObjects<Profile> { Profile(it.optString("n"), it.optString("a").takeIf { s -> s.isNotBlank() }, it.optString("s")) }.orEmpty(),
        j.optJSONArray("l")?.mapObjects<Track> { Track(it.optString("i"), it.optString("t"), it.optString("r"), it.optString("h").takeIf { s -> s.isNotBlank() }, it.optString("s")) }.orEmpty(),
        j.optJSONArray("y")?.mapObjects<PlaylistItem> { PlaylistItem(it.optString("i"), it.optString("t"), it.optInt("c"), it.optString("h").takeIf { s -> s.isNotBlank() }, it.optString("s")) }.orEmpty(),
        j.optLong("t"),
    )
}
