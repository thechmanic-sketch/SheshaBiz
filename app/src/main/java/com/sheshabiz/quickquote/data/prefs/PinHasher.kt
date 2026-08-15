package com.sheshabiz.quickquote.data.prefs

import java.security.MessageDigest

/** One-way hash for the app-lock PIN so the raw digits are never persisted. */
object PinHasher {
    private const val SALT = "SheshaBiz-AppLock-v1"

    fun hash(pin: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest((SALT + pin).toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }
}
