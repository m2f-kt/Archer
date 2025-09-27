package com.m2f.archer.utils

import com.m2f.archer.configuration.Configuration
import com.m2f.archer.crud.cache.CacheDataSource
import com.m2f.archer.crud.cache.configuration.testConfiguration
import com.m2f.archer.crud.cache.memcache.CacheMetaInformation
import com.m2f.archer.failure.Failure
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.time.Duration

class ArcherTestContext(
    configuration: Configuration,
    private val scheduler: TestCoroutineScheduler
) : Configuration() {

    @OptIn(ExperimentalCoroutinesApi::class)
    fun advanceTimeBy(duration: Duration) {
        scheduler.advanceTimeBy(duration)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun getCurrentTime(): Instant = Instant.fromEpochMilliseconds(scheduler.currentTime)

    override val mainFallbacks: (Failure) -> Boolean = configuration.mainFallbacks
    override val storageFallbacks: (Failure) -> Boolean = configuration.storageFallbacks
    override val ignoreCache: Boolean = configuration.ignoreCache
    override val cache: CacheDataSource<CacheMetaInformation, Instant> = configuration.cache
}

fun runArcherTest(
    context: CoroutineContext = EmptyCoroutineContext,
    configuration: (TestCoroutineScheduler) -> Configuration = testConfiguration,
    block: suspend ArcherTestContext.() -> Unit
) =
    runTest(context = context) {
        block(ArcherTestContext(configuration = configuration(testScheduler), scheduler = testScheduler))
    }
