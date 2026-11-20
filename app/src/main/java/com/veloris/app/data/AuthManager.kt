package com.veloris.app.data

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.util.Base64
import android.widget.Toast
import androidx.browser.customtabs.CustomTabsIntent
import com.veloris.app.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.security.MessageDigest
import java.security.SecureRandom

enum class Provider(val id: String, val label: String, val authUrl: String, val tokenUrl: String, val scopes: String, val redirect: String) {
    YOUTUBE("youtube", "YouTube", "https://accounts.google.com/o/oauth2/v2/auth", "https://oauth2.googleapis.com/token",
        "https://www.googleapis.com/auth/youtube.readonly", "com.veloris.app:/oauth2redirect"),
    SPOTIFY("spotify", "Spotify", "https://accounts.spotify.com/authorize", "https://accounts.spotify.com/api/token",
        "user-read-private user-library-read playlist-read-private", "veloris://callback"),
}

/** OAuth 2.0 Authorization Code + PKCE untuk Google (YouTube) dan Spotify. */
object AuthManager {
    private lateinit var prefs: SharedPreferences
    val linked = MutableStateFlow<Set<Provider>>(emptySet())

    fun init(c: Context) {
        if (::prefs.isInitialized) return
        prefs = c.applicationContext.getSharedPreferences("veloris_auth", Context.MODE_PRIVATE)
        refreshLinked()
    }

    private fun refreshLinked() {
        linked.value = Provider.values().filter { prefs.contains("${it.id}_refresh") || prefs.contains("${it.id}_access") }.toSet()
    }

    private fun clientId(p: Provider) = if (p == Provider.SPOTIFY) BuildConfig.SPOTIFY_CLIENT_ID else BuildConfig.GOOGLE_CLIENT_ID
    private fun b64(b: ByteArray) = Base64.encodeToString(b, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)

    fun start(ctx: Context, p: Provider) {
        val cid = clientId(p)
        if (cid.isBlank()) {
            Toast.makeText(ctx, "Client ID ${p.label} belum diatur (lihat README)", Toast.LENGTH_LONG).show()
            return
        }
        val verifier = b64(ByteArray(48).also { SecureRandom().nextBytes(it) })
        val challenge = b64(MessageDigest.getInstance("SHA-256").digest(verifier.toByteArray()))
        prefs.edit().putString("verifier_${p.id}", verifier).apply()
        val b = Uri.parse(p.authUrl).buildUpon()
            .appendQueryParameter("response_type", "code")
            .appendQueryParameter("client_id", cid)
            .appendQueryParameter("redirect_uri", p.redirect)
            .appendQueryParameter("scope", p.scopes)
            .appendQueryParameter("code_challenge_method", "S256")
            .appendQueryParameter("code_challenge", challenge)
            .appendQueryParameter("state", p.id)
        if (p == Provider.YOUTUBE) b.appendQueryParameter("access_type", "offline").appendQueryParameter("prompt", "consent")
        CustomTabsIntent.Builder().build().launchUrl(ctx, b.build())
    }

    suspend fun handleRedirect(uri: Uri): Boolean {
        val p = Provider.values().firstOrNull { it.id == uri.getQueryParameter("state") } ?: return false
        val code = uri.getQueryParameter("code") ?: return false
        val verifier = prefs.getString("verifier_${p.id}", null) ?: return false
        withContext(Dispatchers.IO) {
            val r = JSONObject(Http.postForm(p.tokenUrl, mapOf(
                "grant_type" to "authorization_code", "code" to code, "redirect_uri" to p.redirect,
                "client_id" to clientId(p), "code_verifier" to verifier)))
            save(p, r)
        }
        refreshLinked()
        return true
    }

    private fun save(p: Provider, r: JSONObject) {
        val e = prefs.edit()
        e.putString("${p.id}_access", r.getString("access_token"))
        e.putLong("${p.id}_exp", System.currentTimeMillis() + r.optLong("expires_in", 3600) * 1000 - 60_000)
        if (r.has("refresh_token")) e.putString("${p.id}_refresh", r.getString("refresh_token"))
        e.apply()
    }

    /** Token valid; otomatis refresh jika kedaluwarsa. */
    suspend fun token(p: Provider): String? = withContext(Dispatchers.IO) {
        val acc = prefs.getString("${p.id}_access", null)
        if (acc != null && System.currentTimeMillis() < prefs.getLong("${p.id}_exp", 0)) return@withContext acc
        val rt = prefs.getString("${p.id}_refresh", null) ?: return@withContext null
        runCatching {
            val r = JSONObject(Http.postForm(p.tokenUrl, mapOf("grant_type" to "refresh_token", "refresh_token" to rt, "client_id" to clientId(p))))
            save(p, r)
            r.getString("access_token")
        }.getOrNull()
    }

    fun logout(p: Provider) {
        prefs.edit().remove("${p.id}_access").remove("${p.id}_exp").remove("${p.id}_refresh").remove("verifier_${p.id}").apply()
        refreshLinked()
        LibraryRepository.removeSource(p.id)
    }
}
