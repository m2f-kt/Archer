package com.m2f.archer.datasource.concurrency

import arrow.core.Either
import com.m2f.archer.datasource.DataSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.IO
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

fun <F, Q, A> DataSource<F, Q, A>.mutex(): DataSource<F, Q, A> =
    object : DataSource<F, Q, A> {
        val mutex by lazy { Mutex() }
        override suspend fun invoke(q: Q): Either<F, A> = mutex.withLock { this@mutex.invoke(q) }
    }

@OptIn(ExperimentalCoroutinesApi::class)
fun <F, Q, A> DataSource<F, Q, A>.parallelism(parallelism: Int): DataSource<F, Q, A> =
    object : DataSource<F, Q, A> {
        val dispatcher = Dispatchers.IO.limitedParallelism(parallelism)
        override suspend fun invoke(q: Q): Either<F, A> = withContext(dispatcher) { this@parallelism.invoke(q) }
    }
