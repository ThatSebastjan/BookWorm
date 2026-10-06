package com.dreamteam.bookworm

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform