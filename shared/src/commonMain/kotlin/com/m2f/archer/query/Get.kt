package com.m2f.archer.query

data class Get<K>(override val key: K) : KeyQuery<K, Nothing>(key)
