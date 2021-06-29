package com.buildercoin

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.TypeAdapter
import com.google.gson.reflect.TypeToken
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import java.io.Reader
import java.util.Base64

private object Base64TypeAdapter : TypeAdapter<ByteArray?>() {
    override fun write(out: JsonWriter, value: ByteArray?) {
        out.value(Base64.getEncoder().withoutPadding().encodeToString(value))
    }

    override fun read(`in`: JsonReader): ByteArray? {
        return Base64.getDecoder().decode(`in`.nextString())
    }
}

val GSON: Gson = GsonBuilder().registerTypeAdapter(ByteArray::class.java, Base64TypeAdapter).create()

inline fun <reified T> Gson.fromJson(json: String) = fromJson<T>(json, object : TypeToken<T>() {}.type)
inline fun <reified T> Gson.fromJson(reader: Reader) = fromJson<T>(reader, object : TypeToken<T>() {}.type)
