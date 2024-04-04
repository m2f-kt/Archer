package com.m2f.archer.crud.cache

import com.m2f.archer.crud.cache.generator.deleteKeyQuery
import com.m2f.archer.crud.cache.generator.getKeyQuery
import com.m2f.archer.crud.cache.generator.putKeyQuery
import com.m2f.archer.crud.either
import com.m2f.archer.datasource.InMemoryDataSource
import com.m2f.archer.failure.DataNotFound
import com.m2f.archer.query.Delete
import com.m2f.archer.query.Get
import com.m2f.archer.query.Put
import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.bind
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.string
import io.kotest.property.checkAll

class InMemoryDataSourceTest : FunSpec({

    test("Adding Some object returns the added object") {
        val dataSource: InMemoryDataSource<String, Int> = InMemoryDataSource()
        val valueToAdd = 1
        val queries = Arb.putKeyQuery(Arb.string(), valueToAdd)

        checkAll(queries) { putQuery ->
            val result = either { dataSource.run { invoke(putQuery) } }
            result shouldBeRight valueToAdd
        }
    }

    test("The output of the DataSource is the same using Put and Get queries") {
        val dataSource: InMemoryDataSource<String, Int> = InMemoryDataSource()
        val queries = Arb.bind(
            Arb.string(),
            Arb.int(),
        ) { k, v -> Get(k) to Put(k, v) }

        // Put is performing a side effect so the oder of call matters.
        // If we call a get before putting the value, the result will be DataNotFound
        checkAll(queries) { (getQuery, putQuery) ->
            either {
                val putResult = dataSource.run { invoke(putQuery) }
                val getResult = dataSource.run { invoke(getQuery) }
                putResult shouldBe getResult
            }
        }
    }

    test("Getting an un-existing value returns a DataNotFound") {
        val dataSource: InMemoryDataSource<String, Int> = InMemoryDataSource()
        val queries = Arb.getKeyQuery(Arb.string())

        checkAll(queries) { getQuery ->
            val result = either { dataSource.get(getQuery.key) }
            result shouldBeLeft DataNotFound
        }
    }

    test("removing an un-existing value just runs") {
        val dataSource: InMemoryDataSource<String, Int> = InMemoryDataSource()
        val queries = Arb.deleteKeyQuery(Arb.string())

        checkAll(queries) { deleteQuery ->
            val result = either { dataSource.run { delete(deleteQuery) } }
            result shouldBeRight Unit
        }
    }

    test("removing an existing value returns the success branch with Unit") {
        val dataSource: InMemoryDataSource<String, Int> = InMemoryDataSource()
        val queries = Arb.bind(
            Arb.string(),
            Arb.int(),
        ) { k, v -> Delete(k) to Put(k, v) }

        checkAll(queries) { (deleteQuery, putQuery) ->
            val deleteResult = either {
                dataSource.run {
                    invoke(putQuery)
                    delete(deleteQuery)
                }
            }
            deleteResult shouldBeRight Unit
        }
    }

    test("removing an existing value removes the value from the data source") {
        val dataSource: InMemoryDataSource<String, Int> = InMemoryDataSource()
        val queries = Arb.bind(
            Arb.string(),
            Arb.int(),
        ) { k, v ->
            Triple(
                Get(k),
                Put(k, v),
                Delete(k),
            )
        }

        checkAll(queries) { (getQuery, putQuery, deleteQuery) ->
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
})
