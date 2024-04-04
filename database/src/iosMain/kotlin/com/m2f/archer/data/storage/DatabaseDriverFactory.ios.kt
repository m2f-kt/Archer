package com.m2f.archer.data.storage

import app.cash.sqldelight.async.coroutines.synchronous
import app.cash.sqldelight.db.QueryResult.AsyncValue
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlSchema
import app.cash.sqldelight.driver.native.NativeSqliteDriver

actual class DatabaseDriverFactory {
    actual companion object {
        actual suspend fun createDriver(schema: SqlSchema<AsyncValue<Unit>>): SqlDriver =
            NativeSqliteDriver(schema.synchronous(), "$databaseName.db")
    }
}
