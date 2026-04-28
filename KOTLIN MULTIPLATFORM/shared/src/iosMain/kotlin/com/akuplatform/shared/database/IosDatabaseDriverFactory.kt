package com.akuplatform.shared.database

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver

/**
 * iOS/Native implementation of [DatabaseDriverFactory].
 *
 * Creates a [NativeSqliteDriver] backed by the on-device SQLite database named [DB_NAME].
 */
class IosDatabaseDriverFactory : DatabaseDriverFactory {

    override fun createDriver(): SqlDriver =
        NativeSqliteDriver(AkuDatabase.Schema, DB_NAME)

    companion object {
        const val DB_NAME = "aku_db"
    }
}
