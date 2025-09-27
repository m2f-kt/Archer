package com.m2f.archer.crud.cache

import arrow.core.None
import arrow.core.Some
import com.m2f.archer.crud.getDataSource
import com.m2f.archer.failure.DataNotFound
import com.m2f.archer.utils.runArcherTest
import io.kotest.matchers.shouldBe
import kotlin.test.Test

class OptionTest {

    @Test
    fun `regular call wraps into Some content`() = runArcherTest {
        val getDataSource = getDataSource<Int, String> { "Success" }
        val result = option { getDataSource.get(0) }
        result shouldBe Some("Success")
    }

    @Test
    fun `failure call wraps into None`() = runArcherTest {
        val getDataSource = getDataSource<Int, String> { raise(DataNotFound) }
        val result = option { getDataSource.get(0) }
        result shouldBe None
    }
}
