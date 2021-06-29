package com.buildercoin

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.google.common.io.BaseEncoding
import java.security.MessageDigest

private val MAPPER = jacksonObjectMapper().enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS)!!

fun Any.hash() = MAPPER.writeValueAsString(this).hash()

fun String.hash(algorithm: String = "SHA-256"): Hash {
    val messageDigest = MessageDigest.getInstance(algorithm)
    messageDigest.update(this.toByteArray())
    return BaseEncoding.base64().encode(messageDigest.digest())
}
