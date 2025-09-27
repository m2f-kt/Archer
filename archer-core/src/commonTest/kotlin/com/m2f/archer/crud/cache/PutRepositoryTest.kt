package com.m2f.archer.crud.cache

import com.m2f.archer.crud.PutDataSource
import com.m2f.archer.crud.putDataSource
import com.m2f.archer.repository.toRepository
import com.m2f.archer.utils.runArcherTest
import io.kotest.matchers.shouldBe
import kotlin.test.Test

class PutRepositoryTest {

    @Test
    fun `A PutRepository Calling put The result should be the same as the put data source`() = runArcherTest {
        val putDataSource: PutDataSource<Int, String> =
            putDataSource { key, value -> "put $value with key $key" }
        val repository = putDataSource.toRepository()

        val result = either { repository.put(1, "value") }
        val expected = either { putDataSource.put(1, "value") }

        result shouldBe expected
    }

    @Test
    fun `A PutRepository without value calling put passing a single value should return the same as the put data source passing Unit as value`() = runArcherTest {
        val postDataSource: PutDataSource<Int, Unit> = putDataSource { key, _ -> "$key" }
        val repository = postDataSource.toRepository()

        val result = either { repository.put(1) }
        val expected = either { postDataSource.put(1) }

        result shouldBe expected
    }
}
