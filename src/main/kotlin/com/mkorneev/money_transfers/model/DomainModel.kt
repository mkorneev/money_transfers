package com.mkorneev.money_transfers.model

import arrow.core.Either
import arrow.core.Option
import arrow.core.Try
import com.mkorneev.money_transfers.impl.Update
import com.squareup.moshi.JsonClass
import org.javamoney.moneta.Money
import java.time.Instant
import java.time.LocalDate
import javax.money.CurrencyUnit

typealias AccountNumber = String
typealias TransactionId = String

@JsonClass(generateAdapter = true)
data class Holder(val fullName: String, val address: String)

@JsonClass(generateAdapter = true)
data class Account(val number: AccountNumber,
                   val holder: Holder,
                   val openedAt: LocalDate, val closedAt: Option<LocalDate>,
                   val balance: Money) {
    val currency: CurrencyUnit = balance.currency
}

@JsonClass(generateAdapter = true)
data class AccountOpenRequest(val holder: Holder, val currency: CurrencyUnit)


@JsonClass(generateAdapter = true)
data class AccountTransaction(val id: TransactionId, val from: AccountNumber, val to: AccountNumber,
                              val amount: Money, val timestamp: Instant, val message: String)

@JsonClass(generateAdapter = true)
data class TransferRequest @JvmOverloads constructor(
        val from: AccountNumber, val to: AccountNumber, val amount: Money,
        val message: String = "", val datetime: Instant = Instant.now())

@JsonClass(generateAdapter = true)
data class DepositRequest @JvmOverloads constructor(
        val accountNumber: AccountNumber, val amount: Money,
        val message: String = "", val datetime: Instant = Instant.now())

@JsonClass(generateAdapter = true)
data class WithdrawRequest @JvmOverloads constructor(
        val accountNumber: AccountNumber, val amount: Money,
        val message: String = "", val datetime: Instant = Instant.now())


// Errors

sealed class OpenError {
    data class RepositoryException(val throwable: Throwable) : OpenError()
    object DuplicateId: OpenError()
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

data class NotFound<IdType>(val id: IdType)
object DuplicateId

interface Repository<V, IdType> {
    fun query(id: IdType): Try<Either<NotFound<IdType>, V>>
    fun exists(id: IdType): Try<Boolean>
    fun create(id: IdType, value: V): Either<DuplicateId, V>
    fun <E> transform1(id: IdType, f: (V) -> Either<E, V>):
            Either<Either<NotFound<IdType>, E>, Account>
    fun <E> transform(updates: List<Update<E>>): Either<Either<NotFound<IdType>, E>, List<V>>
}

abstract class AccountRepository : Repository<Account, AccountNumber>
abstract class TransactionRepository : Repository<AccountTransaction, TransactionId>

// Services

interface IAccountService {
    fun openAccount(holder: Holder, currency: CurrencyUnit, openedAt: LocalDate): Either<OpenError, Account>
    fun getDetails(accountNumber: AccountNumber): Either<BalanceError, Account>

    fun withdraw(accountNumber: AccountNumber, amount: Money): Either<TransferError, Account>
    fun deposit(accountNumber: AccountNumber, amount: Money): Either<TransferError, Account>
    fun transfer(request: TransferRequest): Either<TransferError, AccountTransaction>
    fun balance(accountNumber: AccountNumber): Either<BalanceError, Money>
}