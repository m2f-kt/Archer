package com.m2f.archer.crud.cache.generator

import com.m2f.archer.query.Delete
import com.m2f.archer.query.Get
import com.m2f.archer.query.KeyQuery
import com.m2f.archer.query.Put
import io.kotest.property.Arb
import io.kotest.property.arbitrary.arbitrary
import io.kotest.property.arbitrary.arbitraryBuilder
import io.kotest.property.arbitrary.map
import io.kotest.property.arbitrary.next

fun <K> Arb.Companion.getKeyQuery(key: Arb<K>): Arb<Get<K>> = key.map { Get(it) }
fun <K, A> Arb.Companion.putKeyQuery(
    key: Arb<K>,
    value: Arb<A>,
): Arb<Put<K, A>> = arbitrary { Put(key.bind(), value.bind()) }
fun <K, A> Arb.Companion.putKeyQuery(
    key: Arb<K>,
    value: A,
): Arb<Put<K, A>> = arbitrary { Put(key.bind(), value) }
fun <K> Arb.Companion.deleteKeyQuery(key: Arb<K>): Arb<Delete<K>> = key.map { Delete(it) }

fun <K, A> Arb.Companion.keyQuery(key: Arb<K>, value: Arb<A>): Arb<KeyQuery<K, out A>> = arbitraryBuilder {
    when (it.random.nextInt().rem(3)) {
        0 -> Arb.getKeyQuery<K>(key).next(it)
        else -> Arb.putKeyQuery(key, value).next(it)
    }
}
