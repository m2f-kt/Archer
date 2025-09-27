package com.m2f.archer.crud.cache

import com.m2f.archer.crud.getDataSource
import com.m2f.archer.crud.validate.validate
import com.m2f.archer.failure.Invalid
import com.m2f.archer.utils.runArcherTest
import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import kotlin.test.Test

class ValidationTest {

    @Test
    fun `always valid`() = runArcherTest {
        val get = getDataSource<Unit, String> { "hello" }
            .validate { true }

        val result = either { get.get(Unit) }
        result shouldBeRight "hello"
    }

    @Test
    fun `always invalid`() = runArcherTest {
        val get = getDataSource<Unit, String> { "hello" }
            .validate { false }

        val result = either { get.get(Unit) }
        result shouldBeLeft Invalid
    }
}