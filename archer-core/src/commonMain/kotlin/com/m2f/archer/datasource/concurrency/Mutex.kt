package com.m2f.archer.datasource.concurrency

import com.m2f.archer.crud.ArcherRaise
import com.m2f.archer.datasource.DataSource
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

fun <Q, A> DataSource<Q, A>.mutex(): DataSource<Q, A> =
    object : DataSource<Q, A> {
        val mutex by lazy { Mutex() }
        override suspend fun ArcherRaise.invoke(q: Q): A = mutex.withLock { this@mutex.run { invoke(q) } }
    }

@OptIn(ExperimentalCoroutinesApi::class)
expect fun <Q, A> DataSource<Q, A>.parallelism(parallelism: Int): DataSource<Q, A>
