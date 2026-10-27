package com.dreamteam.bookworm.data.remote

import android.content.Context

/** Keeps the session in the app's private SharedPreferences, which other apps can't read. */
class AndroidSessionStore(context: Context) : SessionStore {
    private val prefs = context.getSharedPreferences("bookworm_session", Context.MODE_PRIVATE)

    override fun read(): String? = prefs.getString(KEY, null)
    override fun write(value: String) = prefs.edit().putString(KEY, value).apply()
    override fun clear() = prefs.edit().remove(KEY).apply()

    private companion object {
        const val KEY = "session"
    }
}
