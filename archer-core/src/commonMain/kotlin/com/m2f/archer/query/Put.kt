package com.m2f.archer.query

data class Put<K, A>(override val key: K, val value: A?) : KeyQuery<K, A>(key)

fun <K, A, B> Put<K, A>.map(body: (A) -> B): Put<K, B> = Put(key, value?.let(body))
