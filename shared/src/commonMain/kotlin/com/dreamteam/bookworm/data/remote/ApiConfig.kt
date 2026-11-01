package com.dreamteam.bookworm.data.remote

/**
 * Where the BookWorm backend (`backend/`, port 3000) is reached from each platform
 * during development. The Android emulator sees the host machine as 10.0.2.2.
 */
expect val defaultApiBaseUrl: String
