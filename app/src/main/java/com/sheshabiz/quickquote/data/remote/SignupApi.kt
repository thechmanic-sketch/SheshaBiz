package com.sheshabiz.quickquote.data.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Fire-and-forget trial-signup capture: records that a business started using the app,
 * purely for the owner's own trial/anti-abuse records. No login, password, or session
 * results from this call — it is a one-way insert, never read back by the app.
 */
object SignupApi {
    private const val SUPABASE_URL = "https://djeipbwcyfaxjlllazbl.supabase.co"
    private const val SUPABASE_ANON_KEY = "sb_publishable_nuoqwOSuf6pJcTaj9lrrDg_hxgLHiHK"

    suspend fun submitSignup(
        businessName: String,
        contact: String,
        contactType: String,
        country: String
    ) = withContext(Dispatchers.IO) {
        runCatching {
            val body = JSONObject().apply {
                put("business_name", businessName)
                put("contact", contact)
                put("contact_type", contactType)
                put("country", country)
                put("platform", "android")
                fetchPublicIp()?.let { put("ip_address", it) }
            }
            postJson("$SUPABASE_URL/rest/v1/signups", body.toString())
        }
    }

    private fun fetchPublicIp(): String? = runCatching {
        val connection = (URL("https://api.ipify.org?format=json").openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 4000
            readTimeout = 4000
        }
        val text = connection.inputStream.bufferedReader().use { reader -> reader.readText() }
        JSONObject(text).optString("ip", null)
    }.getOrNull()

    private fun postJson(urlString: String, json: String) {
        val connection = (URL(urlString).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            doOutput = true
            connectTimeout = 4000
            readTimeout = 4000
            setRequestProperty("apikey", SUPABASE_ANON_KEY)
            setRequestProperty("Authorization", "Bearer $SUPABASE_ANON_KEY")
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Prefer", "return=minimal")
        }
        connection.outputStream.use { it.write(json.toByteArray()) }
        connection.responseCode
        connection.disconnect()
    }
}
