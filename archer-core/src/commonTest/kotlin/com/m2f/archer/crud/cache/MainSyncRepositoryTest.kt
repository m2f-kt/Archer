package com.m2f.archer.crud.cache

import com.m2f.archer.crud.cache.CacheExpiration.Never
import com.m2f.archer.crud.getDataSource
import com.m2f.archer.crud.operation.MainSync
import com.m2f.archer.crud.plus
import com.m2f.archer.crud.putDataSource
import com.m2f.archer.failure.DataEmpty
import com.m2f.archer.failure.DataNotFound
import com.m2f.archer.failure.Unhandled
import com.m2f.archer.repository.MainSyncRepository
import com.m2f.archer.utils.runArcherTest
import io.kotest.assertions.arrow.core.shouldBeLeft
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class MainSyncRepositoryTest {

    val getException = Exception("Get")
    val putException = Exception("Put")

    val failGet = getDataSource<Int, String> { throw getException }
    val raiseGet = getDataSource<Int, String> { raise(DataEmpty) }
    val failPut = putDataSource<Int, String> { _, _ -> throw putException }
    val put = putDataSource<Int, String> { _, _ -> "Put" }

    val storeFailAll = failGet + failPut
    val storeFailGet = failGet + put
    val storeRaiseGet = raiseGet + put

    val mainDataSource = getDataSource<Int, String> { "Main" }
    val failMainDataSource = getDataSource<Int, String> { raise(DataNotFound) }

    @Test
    fun `Repository will catch any exception thrown by the store put`() = runTest {
        runArcherTest {
            val repository = MainSyncRepository(storeDataSource = storeFailAll, mainDataSource = mainDataSource)

            val result = either { repository.get(0) }

            result shouldBeLeft Unhandled(putException)
        }
    }

    @Test
    fun `Repository will catch any exception thrown by the store get`() = runTest {
        runArcherTest {
            val repository = failMainDataSource cacheWith storeFailGet expires Never

            val result = either { repository.get(MainSync, 0) }

            result shouldBeLeft Unhandled(getException)
        }
    }

    @Test
    fun `Repository will fallback any failure raise by the store get`() = runTest {
        runArcherTest {
            val repository = failMainDataSource cacheWith storeRaiseGet expires Never

            val result = either { repository.get(MainSync, 0) }

            result shouldBeLeft DataNotFound
        }
    }
}
