package com.mkorneev.money_transfers.impl

import arrow.core.*
import com.mkorneev.money_transfers.model.*
import java.util.*

// Is not exposed via API

class TransactionRepositoryInMemory : TransactionRepository() {
    private val transactions: MutableMap<TransactionId, AccountTransaction> = HashMap()

    override fun query(id: TransactionId): Try<Either<NotFound<TransactionId>, AccountTransaction>> =
            Success(transactions[id].rightIfNotNull { NotFound(id) }.map { it })

    override fun create(id: TransactionId, value: AccountTransaction): Either<DuplicateId, AccountTransaction> =
            synchronized(transactions) {
                if (transactions.containsKey(id)) {
                    DuplicateId.left()
                } else {
                    transactions[id] = value
                    value.right()
                }
            }

    override fun exists(id: AccountNumber): Try<Boolean> = Success(transactions.containsKey(id))

    override fun <E> transform(updates: List<Update<E>>): Either<Either<NotFound<TransactionId>, E>, List<AccountTransaction>> {
        throw NotImplementedError("not applicable")
    }

    override fun <E> transform1(id: TransactionId, f: (AccountTransaction) -> Either<E, AccountTransaction>): Either<Either<NotFound<TransactionId>, E>, Account> {
        throw NotImplementedError("not applicable")
    }

}

fun getUniqueTransactionId(): TransactionId = UUID.randomUUID().toString()