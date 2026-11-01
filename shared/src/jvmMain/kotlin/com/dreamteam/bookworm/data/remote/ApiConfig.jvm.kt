package com.dreamteam.bookworm.data.remote

/** Set `BOOKWORM_API_URL` to use another backend, e.g. `http://localhost:3001`. */
actual val defaultApiBaseUrl: String = System.getenv("BOOKWORM_API_URL")?.takeIf { it.isNotBlank() } ?: "http://localhost:3000"
