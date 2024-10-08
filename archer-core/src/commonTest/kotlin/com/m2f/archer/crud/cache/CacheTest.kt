package com.m2f.archer.crud.cache

import com.m2f.archer.crud.GetDataSource
import com.m2f.archer.crud.PutDataSource
import com.m2f.archer.crud.StoreDataSource
import com.m2f.archer.crud.getDataSource
import com.m2f.archer.crud.operation.Main
import com.m2f.archer.crud.operation.MainSync
import com.m2f.archer.crud.operation.Store
import com.m2f.archer.crud.operation.StoreSync
import com.m2f.archer.crud.plus
import com.m2f.archer.crud.putDataSource
import com.m2f.archer.failure.DataNotFound
import com.m2f.archer.repository.MainSyncRepository
import com.m2f.archer.repository.SingleDataSourceRepository
import com.m2f.archer.repository.StoreSyncRepository
import com.m2f.archer.utils.archerTest
import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

class CacheTest : FunSpec({

    archerTest("Calling cache with empty parameter It uses a default InMemorydataSource") {
        val mainGet: GetDataSource<Int, String> = getDataSource { "main" }
        val repository = mainGet.cache()
        either { repository.get(Store, 0) } shouldBeLeft DataNotFound
        either { repository.get(MainSync, 0) } shouldBe either { mainGet.get(0) }
    }

    archerTest("Calling cache passing StoreSync The repository should be StoreSyncRepository") {
        val mainGet: GetDataSource<Int, String> = getDataSource { "main" }
        val get: GetDataSource<Int, String> = getDataSource { "store get" }
        val put: PutDataSource<Int, String> = putDataSource { _, _ -> "store put" }
        val store: StoreDataSource<Int, String> = get + put
        val repository = mainGet.cache(store).create(StoreSync)
        repository.shouldBeInstanceOf<StoreSyncRepository<Int, String>>()
    }

    archerTest("Calling cache passing MainSync The repository should be MainSyncRepository") {
        val mainGet: GetDataSource<Int, String> = getDataSource { "main" }
        val get: GetDataSource<Int, String> = getDataSource { "store get" }
        val put: PutDataSource<Int, String> = putDataSource { _, _ -> "store put" }
        val store: StoreDataSource<Int, String> = get + put
        val repository = mainGet.cache(store).create(MainSync)

        repository.shouldBeInstanceOf<MainSyncRepository<Int, String>>()
    }

    archerTest(
        "Calling cache passing Main The repository should be SingleDataSourceRepository and The repository get output should be the same as the mainGet"
    ) {
        val mainGet: GetDataSource<Int, String> = getDataSource { "main" }
        val get: GetDataSource<Int, String> = getDataSource { "store get" }
        val put: PutDataSource<Int, String> = putDataSource { _, _ -> "store put" }
        val store: StoreDataSource<Int, String> = get + put

        val repository = mainGet.cache(store).create(Main)

        repository.shouldBeInstanceOf<SingleDataSourceRepository<Int, String>>()
        either { repository.get(1) } shouldBe either { mainGet.get(1) }
    }

    archerTest(
        "Calling cache passing Store The repository should be SingleDataSourceRepository and The repository get output should be the same as the store"
    ) {
        val mainGet: GetDataSource<Int, String> = getDataSource { "main" }
        val get: GetDataSource<Int, String> = getDataSource { "store get" }
        val put: PutDataSource<Int, String> = putDataSource { _, _ -> "store put" }
        val store: StoreDataSource<Int, String> = get + put
        val repository = mainGet.cache(store).create(Store)

        repository.shouldBeInstanceOf<SingleDataSourceRepository<Int, String>>()

        either { repository.get(1) } shouldBe either { store.get(1) }
    }
})
