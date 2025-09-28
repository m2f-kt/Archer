package com.m2f.archer.crud.cache.memcache

expect inline fun <reified A> getMetaInformation(key: String): CacheMetaInformation
