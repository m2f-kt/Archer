package com.m2f.archer.datasource.concurrency

import com.m2f.archer.crud.ArcherRaise
import com.m2f.archer.datasource.DataSource
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

fun <F, Q, A> DataSource<F, Q, A>.mutex(): DataSource<F, Q, A> =
    object : DataSource<F, Q, A> {
        val mutex by lazy { Mutex() }
        override suspend fun ArcherRaise.invoke(q: Q): A & Any = mutex.withLock { this@mutex.run { invoke(q) } }
    }

@OptIn(ExperimentalCoroutinesApi::class)
expect fun <F, Q, A> DataSource<F, Q, A>.parallelism(parallelism: Int): DataSource<F, Q, A>
