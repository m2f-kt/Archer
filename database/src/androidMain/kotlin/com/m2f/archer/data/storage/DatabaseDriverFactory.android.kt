package com.m2f.archer.data.storage

import app.cash.sqldelight.async.coroutines.synchronous
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlSchema
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.m2f.archer.startup.applicationContext

actual object DatabaseDriverFactory {
    actual suspend fun createDriver(schema: SqlSchema<QueryResult.AsyncValue<Unit>>): SqlDriver =
        AndroidSqliteDriver(schema.synchronous(), applicationContext, DATABASE_NAME)
}
