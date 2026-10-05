package com.appkomat.skoda.kroq

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform