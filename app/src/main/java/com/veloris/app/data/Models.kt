package com.veloris.app.data

import org.json.JSONArray
import org.json.JSONObject

data class Track(val id: String, val title: String, val artist: String, val thumb: String?, val source: String = "youtube") {
    val url: String get() = if (source == "spotify") "https://open.spotify.com/track/$id" else "https://music.youtube.com/watch?v=$id"
}

data class PlaylistItem(val id: String, val title: String, val count: Int, val thumb: String?, val source: String) {
    val url: String get() = if (source == "spotify") "https://open.spotify.com/playlist/$id" else "https://music.youtube.com/playlist?list=$id"
}

data class Profile(val name: String, val avatar: String?, val source: String)

data class LibraryData(
    val profiles: List<Profile> = emptyList(),
    val liked: List<Track> = emptyList(),
    val playlists: List<PlaylistItem> = emptyList(),
    val syncedAt: Long = 0L,
)

inline fun <T> JSONArray.mapObjects(f: (JSONObject) -> T?): List<T> =
    (0 until length()).mapNotNull { i -> optJSONObject(i)?.let(f) }
