package com.m2f.archer.database

import app.cash.sqldelight.async.coroutines.synchronous
import app.cash.sqldelight.db.QueryResult.AsyncValue
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlSchema
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver

@Suppress("UtilityClassWithPublicConstructor")
internal actual object FakeDatabaseDriverFactory {
    actual suspend fun createDriver(schema: SqlSchema<AsyncValue<Unit>>): SqlDriver =
        JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY).also {
            schema.synchronous().create(it)
        }
}