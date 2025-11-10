package com.example.server

import org.flywaydb.core.Flyway

object Migrator {
    fun migrate(url: String, user: String, password: String) {
        val flyway = Flyway.configure()
            .dataSource(url, user, password)
            .locations("classpath:db/migration")
            .baselineOnMigrate(true)
            .load()
        flyway.migrate()
    }
}
