package com.m2f.archer.data.storage

import app.cash.sqldelight.async.coroutines.synchronous
import app.cash.sqldelight.db.QueryResult.AsyncValue
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlSchema
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.m2f.archer.startup.applicationContext

actual class DatabaseDriverFactory {
    actual companion object {
        actual suspend fun createDriver(schema: SqlSchema<AsyncValue<Unit>>): SqlDriver =
            AndroidSqliteDriver(schema.synchronous(), applicationContext, "$databaseName.db")
    }
}
