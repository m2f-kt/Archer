package com.m2f.archer.crud.cache

import com.m2f.archer.crud.getDataSource
import com.m2f.archer.failure.DataNotFound
import com.m2f.archer.utils.runArcherTest
import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import kotlin.test.Test

class EitherTest {

    @Test
    fun `regular call wraps into Right`() = runArcherTest {
        val getDataSource = getDataSource<Int, String> { "Success" }
        val result = either { getDataSource.get(0) }
        result shouldBeRight "Success"
    }

    @Test
    fun `failure call wraps into None`() = runArcherTest {
        val getDataSource = getDataSource<Int, String> { raise(DataNotFound) }
        val result = either { getDataSource.get(0) }
        result shouldBeLeft DataNotFound
    }
}