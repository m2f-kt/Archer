package com.m2f.archer.crud.cache

import com.m2f.archer.crud.DeleteDataSource
import com.m2f.archer.repository.toDeleteRepository
import com.m2f.archer.utils.runArcherTest
import io.kotest.matchers.booleans.shouldBeTrue
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class DeleteRepository {

    @Test
    fun `A Delete Repository Calling delete data source should be called`() = runTest {
        runArcherTest {
            var deleteCalled = false
            val detleteDataSource = DeleteDataSource<Int> { _ -> deleteCalled = true }
            val repository = detleteDataSource.toDeleteRepository()

            either { repository.delete(1) }

            deleteCalled.shouldBeTrue()
        }
    }
}
