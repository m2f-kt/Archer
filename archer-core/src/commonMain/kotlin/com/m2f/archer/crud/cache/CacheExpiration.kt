package com.m2f.archer.crud.cache

import kotlin.time.Duration

sealed interface CacheExpiration {

    data object Never : CacheExpiration
    data object Always : CacheExpiration
    data class After(val time: Duration) : CacheExpiration
}
