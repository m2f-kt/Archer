package com.m2f.archer.query

sealed interface Query<A>

/**
 * A query that exposes a Key, normally used to identify a resource.
 */
sealed class KeyQuery<K, A>(open val key: K) : Query<A>
