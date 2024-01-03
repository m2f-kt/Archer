package com.m2f.archer.data.storage

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.m2f.archer.sqldelight.CacheExpirationDatabase

actual class DatabaseDriverFactory {
    actual companion object {
        actual fun createDriver(databaseName: String): SqlDriver =
            JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY).also {
                CacheExpirationDatabase.Schema.create(it)
            }
    }

}