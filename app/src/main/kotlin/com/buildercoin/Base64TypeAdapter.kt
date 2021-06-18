package com.buildercoin

import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import java.util.Base64

class Base64TypeAdapter : TypeAdapter<ByteArray?>() {
    override fun write(out: JsonWriter, value: ByteArray?) {
        out.value(Base64.getEncoder().withoutPadding().encodeToString(value))
    }

    override fun read(`in`: JsonReader): ByteArray? {
        return Base64.getDecoder().decode(`in`.nextString())
    }
}