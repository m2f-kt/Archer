package com.m2f.archer.crud.cache

import com.m2f.archer.crud.DeleteDataSource
import com.m2f.archer.crud.deleteDataSource
import com.m2f.archer.failure.DataNotFound
import com.m2f.archer.utils.runArcherTest
import io.kotest.matchers.shouldBe
import kotlin.test.Test

class DeleteDataSourceTest {

    @Test
    fun `Create a delete data source with dsl`() = runArcherTest {
        val deleteDSL = deleteDataSource<Int> { }
        val delete = DeleteDataSource<Int> { }

        val result1 = ice { deleteDSL.delete(1) }
        val result2 = ice { delete.delete(1) }
        result2 shouldBe result1
    }

    @Test
    fun `Create a delete failing data source with dsl`() = runArcherTest {
        val deleteDSL = deleteDataSource<Int> { raise(DataNotFound) }
        val delete = DeleteDataSource<Int> { raise(DataNotFound) }

        val result1 = ice { deleteDSL.delete(1) }
        val result2 = ice { delete.delete(1) }
        result2 shouldBe result1
    }
}
