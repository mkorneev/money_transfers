package com.mkorneev.money_transfers.rest

import arrow.core.Either
import arrow.core.Try
import arrow.core.flatMap
import com.mkorneev.money_transfers.impl.AccountRepositoryInMemory
import com.mkorneev.money_transfers.impl.AccountService
import com.mkorneev.money_transfers.impl.Repositories
import com.mkorneev.money_transfers.impl.TransactionRepositoryInMemory
import com.mkorneev.money_transfers.model.*
import spark.kotlin.RouteHandler
import java.time.LocalDate

object AccountApi {
    private val accountService = AccountService.getService(
            Repositories(AccountRepositoryInMemory(), TransactionRepositoryInMemory()))

    val show = fun RouteHandler.(): Any {
        val accountNumber = request.params(":account")

        return accountService.getDetails(accountNumber)
                .mapLeft { NotFoundError("Account $accountNumber not found") }
                .toResponse(this)
    }

    val balance = fun RouteHandler.(): Any {
        val accountNumber = params(":account")

        return accountService.balance(accountNumber)
                .mapLeft { NotFoundError("Account $accountNumber not found") }
                .toResponse(this)
    }

    val open = fun RouteHandler.(): Any =
            extractFromBody<AccountOpenRequest>(request.body())
                    .flatMap {
                        accountService.openAccount(it.holder, it.currency, LocalDate.now())
                                .mapLeft { AccountOpenError("Account cannot be opened") }
                    }
                    .toResponse(this)

    val deposit = fun RouteHandler.(): Any {
        val accountNumber = params(":account")

        return extractFromBody<DepositRequest>(request.body())
                .flatMap { accountService.deposit(accountNumber, it.amount).mapLeft(::convertError) }
                .toResponse(this)
    }

    val withdraw = fun RouteHandler.(): Any {
        val accountNumber = params(":account")

        return extractFromBody<WithdrawRequest>(request.body())
                .flatMap { accountService.withdraw(accountNumber, it.amount).mapLeft(::convertError) }
                .toResponse(this)
    }

    val transfer = fun RouteHandler.(): Any =
            extractFromBody<TransferRequest>(request.body())
                    .flatMap { accountService.transfer(it).mapLeft(::convertError) }
                    .toResponse(this)

    private inline fun <reified V> extractFromBody(body: String): Either<BadRequestError, V> =
            Try { JsonAdaptors.fromJson<V>(body) }
                    .toEither { BadRequestError(it.message ?: "") }
                    .flatMap { it.toEither { BadRequestError("Cannot parse request") } }

    private fun convertError(it: TransferError): ErrorMessage {
        return when (it) {
            is TransferError.RepositoryException -> GeneralError(it.toString())
            is TransferError.NegativeAmount -> BadRequestError("Cannot transfer negative amounts")
            is TransferError.NoSuchAccount -> BadRequestError("Account ${it.accountNumber} not found")
            is TransferError.DifferentCurrency -> BadRequestError("Currency mismatch")
            is TransferError.InsufficientFunds -> BadRequestError("Insufficient funds")
        }
    }
}


open class ErrorMessage(val code: Int, open val message: String)
data class BadRequestError(@Transient override val message: String) : ErrorMessage(400, message)
data class NotFoundError(@Transient override val message: String) : ErrorMessage(404, message)
data class AccountOpenError(@Transient override val message: String) : ErrorMessage(500, message)
data class GeneralError(@Transient override val message: String) : ErrorMessage(500, message)


inline fun <reified V> Either<ErrorMessage, V>.toResponse(routeHandler: RouteHandler): Any =
        this.fold(
                {
                    routeHandler.status(it.code)
                    JsonAdaptors.toJson(it)
                },
                JsonAdaptors::toJson)
