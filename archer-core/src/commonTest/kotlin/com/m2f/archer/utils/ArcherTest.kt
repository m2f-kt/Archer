package com.m2f.archer.utils

import com.m2f.archer.configuration.Configuration
import com.m2f.archer.configuration.Settings
import com.m2f.archer.crud.cache.configuration.testConfiguration
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.runTest
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.time.Duration
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

class ArcherTestContext(
    settings: Settings,
    private val scheduler: TestCoroutineScheduler
) : Configuration(settings) {

    @OptIn(ExperimentalCoroutinesApi::class)
    fun advanceTimeBy(duration: Duration) {
        scheduler.advanceTimeBy(duration)
    }

    @OptIn(ExperimentalCoroutinesApi::class, ExperimentalTime::class)
    override fun getCurrentTime(): Instant = Instant.fromEpochMilliseconds(scheduler.currentTime)
}

fun runArcherTest(
    context: CoroutineContext = EmptyCoroutineContext,
    settings: (TestCoroutineScheduler) -> Settings = testConfiguration,
    block: suspend ArcherTestContext.() -> Unit
) =
    runTest(context = context) {
        block(ArcherTestContext(settings = settings(testScheduler), scheduler = testScheduler))
    }
