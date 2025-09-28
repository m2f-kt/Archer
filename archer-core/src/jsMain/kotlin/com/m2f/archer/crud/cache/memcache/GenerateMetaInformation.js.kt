package com.m2f.archer.crud.cache.memcache

actual inline fun <reified A> getMetaInformation(key: String): CacheMetaInformation =
    CacheMetaInformation(
        key = key,
        classIdentifier = A::class.let {
            it.simpleName ?: "Unknown"
        }
    )
