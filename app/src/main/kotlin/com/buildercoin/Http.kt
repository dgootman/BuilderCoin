package com.buildercoin

import java.net.URI
import java.time.Duration

inline fun <reified T> URI.get(): T {
    return this.toURL().openConnection().apply {
        connectTimeout = Duration.ofSeconds(1).toMillis().toInt()
        readTimeout = Duration.ofSeconds(1).toMillis().toInt()
    }.getInputStream()
        .use { GSON.fromJson(it.reader()) }
}
