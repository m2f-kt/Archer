package com.m2f.archer.data.storage

import app.cash.sqldelight.db.QueryResult.AsyncValue
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlSchema
import app.cash.sqldelight.driver.worker.WebWorkerDriver
import org.w3c.dom.Worker

actual class DatabaseDriverFactory {
    actual companion object {
        actual suspend fun createDriver(schema: SqlSchema<AsyncValue<Unit>>): SqlDriver {
            return WebWorkerDriver(
                Worker(
                    js("""new URL("@cashapp/sqldelight-sqljs-worker/sqljs.worker.js", import.meta.url)""")
                )
            ).also { schema.create(it).await() }

        }
    }

}
