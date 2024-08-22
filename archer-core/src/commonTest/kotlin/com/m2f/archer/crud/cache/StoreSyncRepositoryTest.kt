package com.m2f.archer.crud.cache

import com.m2f.archer.crud.StoreDataSource
import com.m2f.archer.crud.getDataSource
import com.m2f.archer.failure.Unhandled
import com.m2f.archer.query.Get
import com.m2f.archer.query.Put
import com.m2f.archer.repository.StoreSyncRepository
import com.m2f.archer.utils.archerTest
import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.core.spec.style.FunSpec

class StoreSyncRepositoryTest : FunSpec({

    val getException = Exception("Get")
    val putException = Exception("Put")

    val store = StoreDataSource<Int, String> {
        when (it) {
            is Get -> throw getException
            is Put -> throw putException
        }
    }

    val mainDataSource = getDataSource<Int, String> { "Main" }

    archerTest("Repository will catch any exception thrown by the store.get") {

        val repository = StoreSyncRepository(storeDataSource = store, mainDataSource = mainDataSource)

        val result = either { repository.get(0) }

        result shouldBeLeft Unhandled(getException)
    }
})
