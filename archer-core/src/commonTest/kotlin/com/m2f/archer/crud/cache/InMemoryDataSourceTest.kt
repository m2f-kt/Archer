package com.m2f.archer.crud.cache

import com.m2f.archer.datasource.InMemoryDataSource
import com.m2f.archer.failure.DataNotFound
import com.m2f.archer.query.Delete
import com.m2f.archer.query.Get
import com.m2f.archer.query.Put
import com.m2f.archer.utils.runArcherTest
import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.matchers.shouldBe
import kotlin.test.Test

class InMemoryDataSourceTest {

    @Test
    fun `Adding Some object returns the added object`() = runArcherTest {
        val dataSource: InMemoryDataSource<String, Int> = InMemoryDataSource()
        val valueToAdd = 1
        val keys = listOf("test1", "test2", "key")

        for (key in keys) {
            val putQuery = Put(key, valueToAdd)
            val result = either { dataSource.run { invoke(putQuery) } }
            result shouldBeRight valueToAdd
        }
    }

    @Test
    fun `The output of the DataSource is the same using Put and Get queries`() = runArcherTest {
        val dataSource: InMemoryDataSource<String, Int> = InMemoryDataSource()
        val testCases = listOf(
            "key1" to 42,
            "key2" to 100,
            "test" to -5
        )

        // Put is performing a side effect so the order of call matters.
        // If we call a get before putting the value, the result will be DataNotFound
        for ((key, value) in testCases) {
            val getQuery = Get(key)
            val putQuery = Put(key, value)
            either {
                val putResult = dataSource.run { invoke(putQuery) }
                val getResult = dataSource.run { invoke(getQuery) }
                putResult shouldBe getResult
            }
        }
    }

    @Test
    fun `Getting an un-existing value returns a DataNotFound`() = runArcherTest {
        val dataSource: InMemoryDataSource<String, Int> = InMemoryDataSource()
        val keys = listOf("nonexistent1", "nonexistent2", "missing")

        for (key in keys) {
            val result = either { dataSource.get(key) }
            result shouldBeLeft DataNotFound
        }
    }

    @Test
    fun `removing an un-existing value just runs`() = runArcherTest {
        val dataSource: InMemoryDataSource<String, Int> = InMemoryDataSource()
        val keys = listOf("nonexistent1", "nonexistent2", "missing")

        for (key in keys) {
            val deleteQuery = Delete(key)
            val result = either { dataSource.run { delete(deleteQuery) } }
            result shouldBeRight Unit
        }
    }

    @Test
    fun `removing an existing value returns the success branch with Unit`() = runArcherTest {
        val dataSource: InMemoryDataSource<String, Int> = InMemoryDataSource()
        val testCases = listOf(
            "key1" to 42,
            "key2" to 100,
            "test" to -5
        )

        for ((key, value) in testCases) {
            val deleteQuery = Delete(key)
            val putQuery = Put(key, value)
            val deleteResult = either {
                dataSource.run {
                    invoke(putQuery)
                    delete(deleteQuery)
                }
            }
            deleteResult shouldBeRight Unit
        }
    }

    @Test
    fun `removing an existing value removes the value from the data source`() = runArcherTest {
        val dataSource: InMemoryDataSource<String, Int> = InMemoryDataSource()
        val testCases = listOf(
            "key1" to 42,
            "key2" to 100,
            "test" to -5
        )

        for ((key, value) in testCases) {
            val getQuery = Get(key)
            val putQuery = Put(key, value)
            val deleteQuery = Delete(key)
            val result = either {
                dataSource.run {
                    invoke(putQuery)
                    delete(deleteQuery)
                    invoke(getQuery)
                }
            }
            result shouldBeLeft DataNotFound
        }
    }
}