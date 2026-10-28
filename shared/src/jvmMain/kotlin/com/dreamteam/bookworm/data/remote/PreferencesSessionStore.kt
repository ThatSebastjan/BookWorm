package com.dreamteam.bookworm.data.remote

import java.util.prefs.Preferences

/**
 * Keeps the session in the user's Java preferences (on macOS a plist in ~/Library/Preferences,
 * on Windows the registry). Readable by that OS user only, but not encrypted.
 */
class PreferencesSessionStore(node: String = "com/dreamteam/bookworm/teacher") : SessionStore {
    private val prefs = Preferences.userRoot().node(node)

    override fun read(): String? = prefs.get(KEY, null)

    override fun write(value: String) {
        prefs.put(KEY, value)
        prefs.flush()
    }

    override fun clear() {
        prefs.remove(KEY)
        prefs.flush()
    }

    private companion object {
        const val KEY = "session"
    }
}
