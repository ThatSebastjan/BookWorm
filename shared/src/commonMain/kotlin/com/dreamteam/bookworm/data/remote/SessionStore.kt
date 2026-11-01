package com.dreamteam.bookworm.data.remote

/**
 * Where the signed-in session is kept between launches, as one opaque string.
 * Android uses `AndroidSessionStore`, iOS `KeychainSessionStore`.
 */
interface SessionStore {
    fun read(): String?
    fun write(value: String)
    fun clear()
}

/** Forgets everything when the app closes. Used by the desktop app and tests. */
class InMemorySessionStore : SessionStore {
    private var value: String? = null
    override fun read() = value
    override fun write(value: String) { this.value = value }
    override fun clear() { value = null }
}
