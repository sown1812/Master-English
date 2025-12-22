package com.example.server

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.transaction
import java.sql.Connection

suspend fun <T> dbQuery(
    transactionIsolation: Int = Connection.TRANSACTION_REPEATABLE_READ,
    block: () -> T
): T = withContext(Dispatchers.IO) {
    transaction(transactionIsolation = transactionIsolation) { block() }
}

suspend fun dbPing(): Boolean =
    runCatching {
        dbQuery {
            val stmt = TransactionManager.current().connection.prepareStatement("SELECT 1", false)
            try {
                stmt.executeQuery()
            } finally {
                stmt.closeIfPossible()
            }
            true
        }
    }.getOrDefault(false)
