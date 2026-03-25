@file:OptIn(ExperimentalTime::class)

package com.m2f.archer.crud.cache

import com.m2f.archer.crud.Ice
import com.m2f.archer.crud.StoreDataSource
import com.m2f.archer.crud.getDataSource
import com.m2f.archer.crud.operation.MainSync
import com.m2f.archer.crud.operation.Store
import com.m2f.archer.crud.operation.StoreSync
import com.m2f.archer.datasource.InMemoryDataSource
import com.m2f.archer.query.Get
import com.m2f.archer.query.Put
import com.m2f.archer.utils.runArcherTest
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.time.Duration.Companion.minutes
import kotlin.time.ExperimentalTime

class InvalidateAllCachesTest {

    @Test
    fun `invalidateAllCaches causes next read to go to main instead of store`() = runTest {
        runArcherTest {
            var mainCallCount = 0
            val main = getDataSource<Int, _> {
                mainCallCount++
                "main-$mainCallCount"
            }
            val store: StoreDataSource<Int, String> = InMemoryDataSource()
            val strategy = main cacheWith store expiresIn 5.minutes

            // First call populates cache
            val result1 = ice { strategy.get(MainSync, 1) }
            result1 shouldBe Ice.Content("main-1")

            // Second call should return cached value (from store), not hitting main
            val result2 = ice { strategy.get(StoreSync, 1) }
            result2 shouldBe Ice.Content("main-1")
            mainCallCount shouldBe 1

            // Invalidate ALL caches
            ice { invalidateAllCaches() }

            // Next call should go to main again because expiration was wiped
            val result3 = ice { strategy.get(StoreSync, 1) }
            result3 shouldBe Ice.Content("main-2")
            mainCallCount shouldBe 2
        }
    }

    @Test
    fun `invalidateAllCaches invalidates multiple keys at once`() = runTest {
        runArcherTest {
            var mainCallCount = 0
            val main = getDataSource<Int, _> {
                mainCallCount++
                "value-$it-call$mainCallCount"
            }
            val store: StoreDataSource<Int, String> = InMemoryDataSource()
            val strategy = main cacheWith store expiresIn 5.minutes

            // Populate cache for keys 1, 2, 3
            ice { strategy.get(MainSync, 1) } shouldBe Ice.Content("value-1-call1")
            ice { strategy.get(MainSync, 2) } shouldBe Ice.Content("value-2-call2")
            ice { strategy.get(MainSync, 3) } shouldBe Ice.Content("value-3-call3")
            mainCallCount shouldBe 3

            // All cached, no new main calls
            ice { strategy.get(StoreSync, 1) } shouldBe Ice.Content("value-1-call1")
            ice { strategy.get(StoreSync, 2) } shouldBe Ice.Content("value-2-call2")
            ice { strategy.get(StoreSync, 3) } shouldBe Ice.Content("value-3-call3")
            mainCallCount shouldBe 3

            // Invalidate ALL
            ice { invalidateAllCaches() }

            // All keys should now go to main
            ice { strategy.get(StoreSync, 1) } shouldBe Ice.Content("value-1-call4")
            ice { strategy.get(StoreSync, 2) } shouldBe Ice.Content("value-2-call5")
            ice { strategy.get(StoreSync, 3) } shouldBe Ice.Content("value-3-call6")
            mainCallCount shouldBe 6
        }
    }

    @Test
    fun `invalidateAllCaches invalidates across multiple strategies`() = runTest {
        runArcherTest {
            var mainACount = 0
            var mainBCount = 0
            val mainA = getDataSource<Int, _> {
                mainACount++
                "A-$mainACount"
            }
            val mainB = getDataSource<String, _> {
                mainBCount++
                "B-$mainBCount"
            }
            val storeA: StoreDataSource<Int, String> = InMemoryDataSource()
            val storeB: StoreDataSource<String, String> = InMemoryDataSource()

            val strategyA = mainA cacheWith storeA expiresIn 5.minutes
            val strategyB = mainB cacheWith storeB expiresIn 10.minutes

            // Populate both strategies
            ice { strategyA.get(MainSync, 1) } shouldBe Ice.Content("A-1")
            ice { strategyB.get(MainSync, "x") } shouldBe Ice.Content("B-1")

            // Both cached
            ice { strategyA.get(StoreSync, 1) } shouldBe Ice.Content("A-1")
            ice { strategyB.get(StoreSync, "x") } shouldBe Ice.Content("B-1")
            mainACount shouldBe 1
            mainBCount shouldBe 1

            // Invalidate ALL - should affect both strategies
            ice { invalidateAllCaches() }

            // Both should re-fetch from main
            ice { strategyA.get(StoreSync, 1) } shouldBe Ice.Content("A-2")
            ice { strategyB.get(StoreSync, "x") } shouldBe Ice.Content("B-2")
            mainACount shouldBe 2
            mainBCount shouldBe 2
        }
    }

    @Test
    fun `invalidateAllCaches followed by re-caching works correctly`() = runTest {
        runArcherTest {
            var callCount = 0
            val main = getDataSource<Int, _> {
                callCount++
                "v$callCount"
            }
            val store: StoreDataSource<Int, String> = InMemoryDataSource()
            val strategy = main cacheWith store expiresIn 5.minutes

            // Populate
            ice { strategy.get(MainSync, 1) } shouldBe Ice.Content("v1")

            // Invalidate
            ice { invalidateAllCaches() }

            // Re-fetch (goes to main, re-caches)
            ice { strategy.get(StoreSync, 1) } shouldBe Ice.Content("v2")

            // Now it should be cached again (no new main call)
            ice { strategy.get(StoreSync, 1) } shouldBe Ice.Content("v2")
            callCount shouldBe 2
        }
    }

    @Test
    fun `invalidateAllCaches does not affect single-key invalidateCache`() = runTest {
        runArcherTest {
            var callCount = 0
            val main = getDataSource<Int, _> {
                callCount++
                "v$callCount"
            }
            val store: StoreDataSource<Int, String> = InMemoryDataSource()
            val strategy = main cacheWith store expiresIn 5.minutes

            // Populate keys 1 and 2
            ice { strategy.get(MainSync, 1) }
            ice { strategy.get(MainSync, 2) }
            callCount shouldBe 2

            // Single-key invalidation still works after invalidateAllCaches
            ice { invalidateAllCaches() }

            // Re-populate
            ice { strategy.get(StoreSync, 1) } shouldBe Ice.Content("v3")
            ice { strategy.get(StoreSync, 2) } shouldBe Ice.Content("v4")

            // Now invalidate only key 1
            ice { invalidateCache<String>(1) }

            // Key 1 re-fetches, key 2 stays cached
            ice { strategy.get(StoreSync, 1) } shouldBe Ice.Content("v5")
            ice { strategy.get(StoreSync, 2) } shouldBe Ice.Content("v4")
            callCount shouldBe 5
        }
    }
}

