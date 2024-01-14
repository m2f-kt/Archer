package com.m2f.archer.crud.cache

import com.m2f.archer.crud.cache.generator.deleteKeyQuery
import com.m2f.archer.crud.cache.generator.getKeyQuery
import com.m2f.archer.crud.cache.generator.keyQuery
import com.m2f.archer.crud.cache.generator.putKeyQuery
import com.m2f.archer.datasource.InMemoryDataSource
import com.m2f.archer.failure.DataNotFound
import com.m2f.archer.query.Delete
import com.m2f.archer.query.Get
import com.m2f.archer.query.KeyQuery
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

//class InMemoryDataSourceTest : FunSpec({
//
//    test("InMemoryDataSource returns either right or left") {
//        val dataSource: InMemoryDataSource<String, Int> = InMemoryDataSource()
//        // This is generating several different keys (get, put delete) with different values
//        val getQueries: Arb<KeyQuery<String, out Int>> =
//            Arb.keyQuery(Arb.string(), Arb.int())
//
//        getQueries.checkAll { query ->
//            val result = dataSource(query)
//
//            result.run {
//                (isRight() || isLeft()) shouldBe true
//            }
//        }
//    }
//
//    test("Adding Some object returns the added object") {
//        val dataSource: InMemoryDataSource<String, Int> = InMemoryDataSource()
//        val valueToAdd = 1
//        val queries = Arb.putKeyQuery(Arb.string(), valueToAdd)
//
//        checkAll(queries) { putQuery ->
//            val result = dataSource(putQuery)
//            result shouldBeRight valueToAdd
//        }
//    }
//
//    test("The output of the DataSource is the same using Put and Get queries") {
//        val dataSource: InMemoryDataSource<String, Int> = InMemoryDataSource()
//        val queries = Arb.bind(
//            Arb.string(),
//            Arb.int(),
//        ) { k, v -> Get(k) to Put(k, v) }
//
//        // Put is performing a side effect so the oder of call matters.
//        // If we call a get before putting the value, the result will be DataNotFound
//        checkAll(queries) { (getQuery, putQuery) ->
//            val putResult = dataSource(putQuery)
//            val getResult = dataSource(getQuery)
//            putResult shouldBe getResult
//        }
//    }
//
//    test("Getting an un-existing value returns a DataNotFound") {
//        val dataSource: InMemoryDataSource<String, Int> = InMemoryDataSource()
//        val queries = Arb.getKeyQuery(Arb.string())
//
//        checkAll(queries) { getQuery ->
//            val result = dataSource(getQuery)
//            result shouldBeLeft DataNotFound
//        }
//    }
//
//    test("removing an un-existing value just runs") {
//        val dataSource: InMemoryDataSource<String, Int> = InMemoryDataSource()
//        val queries = Arb.deleteKeyQuery(Arb.string())
//
//        checkAll(queries) { deleteQuery ->
//            val result = dataSource.delete(deleteQuery)
//            result shouldBeRight Unit
//        }
//    }
//
//    test("removing an existing value returns the success branch with Unit") {
//        val dataSource: InMemoryDataSource<String, Int> = InMemoryDataSource()
//        val queries = Arb.bind(
//            Arb.string(),
//            Arb.int(),
//        ) { k, v -> Delete(k) to Put(k, v) }
//
//        checkAll(queries) { (deleteQuery, putQuery) ->
//            dataSource(putQuery)
//            val deleteResult = dataSource.delete(deleteQuery)
//            deleteResult shouldBeRight Unit
//        }
//    }
//
//    test("removing an existing value removes the value from the data source") {
//        val dataSource: InMemoryDataSource<String, Int> = InMemoryDataSource()
//        val queries = Arb.bind(
//            Arb.string(),
//            Arb.int(),
//        ) { k, v ->
//            Triple(
//                Get(k),
//                Put(k, v),
//                Delete(k),
//            )
//        }
//
//        checkAll(queries) { (getQuery, putQuery, deleteQuery) ->
//            dataSource(putQuery)
//            dataSource.delete(deleteQuery)
//            dataSource(getQuery) shouldBeLeft DataNotFound
//        }
//    }
//})
