package com.example.server

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.sql.transactions.transaction
import java.sql.Connection

suspend fun <T> dbQuery(
    transactionIsolation: Int = Connection.TRANSACTION_REPEATABLE_READ,
    block: () -> T
): T = withContext(Dispatchers.IO) {
    transaction(transactionIsolation = transactionIsolation) { block() }
}

