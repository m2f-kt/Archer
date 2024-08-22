package com.m2f.archer.utils

import com.m2f.archer.configuration.Configuration
import com.m2f.archer.crud.cache.CacheDataSource
import com.m2f.archer.crud.cache.configuration.testConfiguration
import com.m2f.archer.crud.cache.memcache.CacheMetaInformation
import com.m2f.archer.failure.Failure
import io.kotest.core.spec.style.scopes.FunSpecRootScope
import io.kotest.core.test.TestScope
import kotlinx.datetime.Instant

class ArcherTest(scope: TestScope, configuration: Configuration) : TestScope by scope, Configuration() {
    override val mainFallbacks: (Failure) -> Boolean = configuration.mainFallbacks
    override val storageFallbacks: (Failure) -> Boolean = configuration.storageFallbacks
    override val ignoreCache: Boolean = configuration.ignoreCache
    override val cache: CacheDataSource<CacheMetaInformation, Instant> = configuration.cache
}

fun FunSpecRootScope.archerTest(
    name: String,
    configuration: Configuration = testConfiguration(),
    block: suspend ArcherTest.() -> Unit
) {

    test(name) {
        block(ArcherTest(this@test, configuration))
    }
}

fun FunSpecRootScope.xarcherTest(
    name: String,
    configuration: Configuration = testConfiguration(),
    block: suspend ArcherTest.() -> Unit
) {

    xtest(name) {
        block(ArcherTest(this@xtest, configuration))
    }
}