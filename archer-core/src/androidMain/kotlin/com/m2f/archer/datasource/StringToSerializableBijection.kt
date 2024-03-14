package com.m2f.archer.datasource

import com.m2f.archer.mapper.Bijection
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

inline fun <reified T> stringToSerializableBijection() = object : Bijection<String, @Serializable T & Any> {

    val json = Json {
        isLenient = true
        ignoreUnknownKeys = true
    }

    override fun from(s: String): T & Any = json.decodeFromString(s)

    override fun to(t: @Serializable T & Any): String = json.encodeToString(t)
}
