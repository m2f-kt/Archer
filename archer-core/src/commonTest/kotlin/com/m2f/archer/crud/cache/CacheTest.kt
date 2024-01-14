package com.m2f.archer.crud.cache

import com.m2f.archer.crud.GetDataSource
import com.m2f.archer.crud.PutDataSource
import com.m2f.archer.crud.StoreDataSource
import com.m2f.archer.crud.get
import com.m2f.archer.crud.getDataSource
import com.m2f.archer.crud.operation.MainOperation
import com.m2f.archer.crud.operation.MainSyncOperation
import com.m2f.archer.crud.operation.StoreOperation
import com.m2f.archer.crud.operation.StoreSyncOperation
import com.m2f.archer.crud.plus
import com.m2f.archer.crud.putDataSource
import com.m2f.archer.failure.DataNotFound
import com.m2f.archer.failure.Failure
import com.m2f.archer.repository.MainSyncRepository
import com.m2f.archer.repository.SingleDataSourceRepository
import com.m2f.archer.repository.StoreSyncRepository
import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

class CacheTest : FunSpec({


    test("calling cache with empty parameter It uses a default InMemorydataSource") {
        val mainGet: GetDataSource<Int, String> = getDataSource { "main" }
        val repository = mainGet.cache()
        repository.get(StoreOperation, 0) shouldBeLeft DataNotFound
        repository.get(MainSyncOperation, 0) shouldBe mainGet.get(0)
    }

    test("Calling cache passing StoreSyncOperation The repository should be StoreSyncRepository") {
        val mainGet: GetDataSource<Int, String> = getDataSource { "main" }
        val get: GetDataSource<Int, String> = getDataSource { "store get" }
        val put: PutDataSource<Int, String> = putDataSource { _, _ -> "store put" }
        val store: StoreDataSource<Int, String> = get + put
        val repository = mainGet.cache(store).create(StoreSyncOperation)
        repository.shouldBeInstanceOf<StoreSyncRepository<Int, String>>()
    }

    test("Calling cache passing MainSyncOperation The repository should be MainSyncRepository") {
        val mainGet: GetDataSource<Int, String> = getDataSource { "main" }
        val get: GetDataSource<Int, String> = getDataSource { "store get" }
        val put: PutDataSource<Int, String> = putDataSource { _, _ -> "store put" }
        val store: StoreDataSource<Int, String> = get + put
        val repository = mainGet.cache(store).create(MainSyncOperation)

        repository.shouldBeInstanceOf<MainSyncRepository<Int, String>>()
    }

    test("Calling cache passing MainOperation The repository should be SingleDataSourceRepository and The repository get output should be the same as the mainGet") {
        val mainGet: GetDataSource<Int, String> = getDataSource { "main" }
        val get: GetDataSource<Int, String> = getDataSource { "store get" }
        val put: PutDataSource<Int, String> = putDataSource { _, _ -> "store put" }
        val store: StoreDataSource<Int, String> = get + put

        val repository = mainGet.cache(store).create(MainOperation)

        repository.shouldBeInstanceOf<SingleDataSourceRepository<Failure, Int, String>>()
        repository.get(1) shouldBe mainGet.get(1)
    }

    test("Calling cache passing StoreOperation The repository should be SingleDataSourceRepository and The repository get output should be the same as the store") {
        val mainGet: GetDataSource<Int, String> = getDataSource { "main" }
        val get: GetDataSource<Int, String> = getDataSource { "store get" }
        val put: PutDataSource<Int, String> = putDataSource { _, _ -> "store put" }
        val store: StoreDataSource<Int, String> = get + put
        val repository = mainGet.cache(store).create(StoreOperation)

        repository.shouldBeInstanceOf<SingleDataSourceRepository<Failure, Int, String>>()

        repository.get(1) shouldBe store.get(1)
    }
})
