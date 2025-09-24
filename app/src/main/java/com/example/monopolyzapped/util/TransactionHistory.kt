package com.example.monopolyzapped.util

import java.util.ArrayDeque

data class TransactionRecord(
    val amountK: Double,
    val payerActor: Int,     // 0..4 (0:O,1:B,2:R,3:V,4:Bank)
    val receiverActor: Int,  // 0..4
    val timestamp: Long
)

object TransactionHistory {
    private val stack = ArrayDeque<TransactionRecord>()

    fun push(rec: TransactionRecord) {
        stack.addLast(rec)
    }

    fun pop(): TransactionRecord? = if (stack.isEmpty()) null else stack.removeLast()

    fun peek(): TransactionRecord? = stack.lastOrNull()

    fun clear() = stack.clear()
}
