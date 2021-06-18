package com.buildercoin

import com.google.common.io.BaseEncoding
import java.security.MessageDigest

fun Any.hash() = GSON.toJson(this).hash()

fun String.hash(algorithm: String = "SHA-256"): Hash {
    val messageDigest = MessageDigest.getInstance(algorithm)
    messageDigest.update(this.toByteArray())
    return BaseEncoding.base64().encode(messageDigest.digest())
}
