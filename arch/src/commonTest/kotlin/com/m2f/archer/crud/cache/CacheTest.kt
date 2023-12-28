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
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

class CacheTest : BehaviorSpec({

    Given("A main get data source and a store data source") {
        val mainGet: GetDataSource<Int, String> = getDataSource { "main" }

        val get: GetDataSource<Int, String> = getDataSource { "store get" }

        val put: PutDataSource<Int, String> = putDataSource { _, _ -> "store put" }

        val store: StoreDataSource<Int, String> = get + put

        When("calling cache with empty parameter") {
            val repository = mainGet.cache()
            Then("It uses a default InMemorydataSource") {
                repository.get(StoreOperation, 0) shouldBeLeft DataNotFound
                repository.get(MainSyncOperation, 0) shouldBe mainGet.get(0)
            }
        }

        When("Calling cache passing StoreSyncOperation") {
            val repository = mainGet.cache(store).create(StoreSyncOperation)

            Then("The repository should be StoreSyncRepository") {
                repository.shouldBeInstanceOf<StoreSyncRepository<Int, String>>()
            }
        }

        When("Calling cache passing MainSyncOperation") {
            val repository = mainGet.cache(store).create(MainSyncOperation)

            Then("The repository should be MainSyncRepository") {
                repository.shouldBeInstanceOf<MainSyncRepository<Int, String>>()
            }
        }

        When("Calling cache passing MainOperation") {
            val repository = mainGet.cache(store).create(MainOperation)

            Then("The repository should be SingleDataSourceRepository") {
                repository.shouldBeInstanceOf<SingleDataSourceRepository<Failure, Int, String>>()
            }

            And("The repository get output should be the same as the mainGet") {
                repository.get(1) shouldBe mainGet.get(1)
            }
        }

        When("Calling cache passing StoreOperation") {
            val repository = mainGet.cache(store).create(StoreOperation)

            Then("The repository should be SingleDataSourceRepository") {
                repository.shouldBeInstanceOf<SingleDataSourceRepository<Failure, Int, String>>()
            }

            And("The repository get output should be the same as the store") {
                repository.get(1) shouldBe store.get(1)
            }
        }
    }
})
