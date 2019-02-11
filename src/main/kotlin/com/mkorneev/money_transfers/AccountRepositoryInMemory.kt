package com.mkorneev.money_transfers

import arrow.core.*

class AccountRepositoryInMemory: AccountRepository() {
    private val accounts: MutableMap<AccountNumber, Account> = HashMap()

    override fun query(id: AccountNumber): Try<Either<NotFound, Account>> =
            Success(accounts[id].rightIfNotNull { NotFound })

    override fun store(value: Account): Try<Account> {
        accounts[value.number] = value
        return Success(value)
    }

    override fun exists(id: AccountNumber): Try<Boolean> = Success(accounts.containsKey(id))
}
