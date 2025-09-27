package com.m2f.archer.crud.cache

import com.m2f.archer.crud.getDataSource
import com.m2f.archer.failure.DataNotFound
import com.m2f.archer.utils.runArcherTest
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import kotlin.test.Test

class NullableTest {

    @Test
    fun `regular call wraps into non null content`() = runArcherTest {
        val getDataSource = getDataSource<Int, String> { "Success" }
        val result = nullable { getDataSource.get(0) }
        result shouldBe "Success"
    }

    @Test
    fun `failure call wraps into null content`() = runArcherTest {
        val getDataSource = getDataSource<Int, String> { raise(DataNotFound) }
        val result = nullable { getDataSource.get(0) }
        result.shouldBeNull()
    }
}