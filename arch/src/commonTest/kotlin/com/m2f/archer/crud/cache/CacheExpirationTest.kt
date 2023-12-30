package com.m2f.archer.crud.cache

import com.m2f.archer.crud.StoreDataSource
import com.m2f.archer.crud.cache.CacheExpiration.After
import com.m2f.archer.crud.cache.CacheExpiration.Always
import com.m2f.archer.crud.cache.CacheExpiration.Never
import com.m2f.archer.crud.get
import com.m2f.archer.crud.put
import com.m2f.archer.datasource.InMemoryDataSource
import com.m2f.archer.failure.Invalid
import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.FunSpec
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

class CacheExpirationTest : FunSpec({

    context("passing expiration") {
        val store: StoreDataSource<Int, String> = InMemoryDataSource(mapOf(0 to "Test"))

        test("never expires") {
            val neverExpires = store.expires(Never)
            neverExpires.get(0) shouldBeRight "Test"
        }

        test("always expires") {
            val alwaysExpires = store.expires(Always)
            alwaysExpires.get(0) shouldBeLeft Invalid
        }

        context("expires with time") {
            val time = 50.milliseconds
            val expiresAfter10Millis = store.expires(After(time))

            test("fetching after time passed") {
                expiresAfter10Millis.put(0, "test10")
                delay(100L)
                expiresAfter10Millis.get(0) shouldBeLeft Invalid
            }

            test("fetching before time passes") {
                expiresAfter10Millis.put(0, "test10_2")
                expiresAfter10Millis.get(0) shouldBeRight "test10_2"
            }
        }
    }
})