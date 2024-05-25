package com.m2f.archer.datasource

import com.m2f.archer.mapper.Bijection
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

inline fun <reified T> stringToSerializableBijection() = object : Bijection<String, @Serializable T> {

    val json = Json {
        isLenient = true
        ignoreUnknownKeys = true
    }

    override fun from(s: String): T = json.decodeFromString(s)

    override fun to(t: @Serializable T): String = json.encodeToString(t)
}
