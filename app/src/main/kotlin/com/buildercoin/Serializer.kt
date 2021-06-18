package com.buildercoin

import com.google.gson.Gson
import com.google.gson.GsonBuilder

val GSON: Gson = GsonBuilder().registerTypeAdapter(ByteArray::class.java, Base64TypeAdapter()).create()