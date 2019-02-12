package com.mkorneev.money_transfers

import arrow.core.Either
import arrow.core.Option
import arrow.core.Try
import org.javamoney.moneta.Money
import java.time.Instant
import java.time.LocalDate
import javax.money.CurrencyUnit

typealias AccountNumber = String
typealias TransactionId = String

data class Holder(val fullName: String, val address: String)

data class Account(val number: AccountNumber,
                   val holder: Holder,
                   val openedAt: LocalDate, val closedAt: Option<LocalDate>,
                   val balance: Money) {
    val currency: CurrencyUnit = balance.currency
}

data class AccountOpenRequest(val holder: Holder, val currency: CurrencyUnit)


data class AccountTransaction(val id: TransactionId, val from: AccountNumber, val to: AccountNumber,
                              val amount: Money, val timestamp: Instant, val message: String)

data class TransferRequest @JvmOverloads constructor(
        val from: AccountNumber, val to: AccountNumber, val amount: Money,
        val datetime: Instant = Instant.now(), val message: String = "")

data class DepositRequest @JvmOverloads constructor(
        val accountNumber: AccountNumber, val amount: Money,
        val datetime: Instant = Instant.now(), val message: String = "")


// Errors

sealed class OpenError {
    data class RepositoryException(val throwable: Throwable) : OpenError()
}

sealed class TransferError {
    object NegativeAmount : TransferError()
    data class RepositoryException(val throwable: Throwable) : TransferError()
    data class NoSuchAccount(val accountNumber: AccountNumber) : TransferError()
    data class DifferentCurrency(val accountNumber: AccountNumber) : TransferError()
    data class InsufficientFunds(val accountNumber: AccountNumber) : TransferError()
}

sealed class BalanceError {
    data class RepositoryException(val throwable: Throwable) : BalanceError()
    object NoSuchAccount : BalanceError()
}

// Repositories

object NotFound

interface Repository<V, IdType> {
    fun query(id: IdType): Try<Either<NotFound, V>>
    fun store(value: V): Try<V>
    fun exists(id: IdType): Try<Boolean>
}

abstract class AccountRepository : Repository<Account, AccountNumber>

// Services

interface IAccountService {
    fun openAccount(holder: Holder, currency: CurrencyUnit, openedAt: LocalDate): Either<OpenError, Account>
    fun getDetails(accountNumber: AccountNumber): Either<BalanceError, Account>

    fun withdraw(accountNumber: AccountNumber, amount: Money): Either<TransferError, Account>
    fun deposit(accountNumber: AccountNumber, amount: Money): Either<TransferError, Account>
    fun transfer(request: TransferRequest): Either<TransferError, AccountTransaction>
    fun balance(accountNumber: AccountNumber): Either<BalanceError, Money>
}