package com.m2f.archer.datasource.concurrency

import arrow.core.Either
import com.m2f.archer.datasource.DataSource
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

fun <F, Q, A> DataSource<F, Q, A>.mutex(): DataSource<F, Q, A> =
    object : DataSource<F, Q, A> {
        val mutex by lazy { Mutex() }
        override suspend fun invoke(q: Q): Either<F, A> = mutex.withLock { this@mutex.invoke(q) }
    }

@OptIn(ExperimentalCoroutinesApi::class)
expect fun <F, Q, A> DataSource<F, Q, A>.parallelism(parallelism: Int): DataSource<F, Q, A>
