package com.veloris.app.data

import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

object Http {
    fun enc(s: String): String = URLEncoder.encode(s, "UTF-8")

    fun get(url: String, bearer: String? = null): String = call(url, "GET", null, bearer)

    fun postForm(url: String, form: Map<String, String>): String =
        call(url, "POST", form.entries.joinToString("&") { "${enc(it.key)}=${enc(it.value)}" }, null)

    private fun call(url: String, method: String, body: String?, bearer: String?): String {
        val c = URL(url).openConnection() as HttpURLConnection
        try {
            c.requestMethod = method
            c.connectTimeout = 10_000
            c.readTimeout = 15_000
            bearer?.let { c.setRequestProperty("Authorization", "Bearer $it") }
            if (body != null) {
                c.doOutput = true
                c.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
                c.outputStream.use { it.write(body.toByteArray()) }
            }
            val code = c.responseCode
            val text = (if (code in 200..299) c.inputStream else c.errorStream)?.bufferedReader()?.use { it.readText() }.orEmpty()
            if (code !in 200..299) throw IOException("HTTP $code: ${text.take(160)}")
            return text
        } finally {
            c.disconnect()
        }
    }
}
