package com.mkorneev.money_transfers

import arrow.core.Either
import arrow.core.Try
import arrow.core.flatMap
import spark.kotlin.RouteHandler
import spark.kotlin.get
import spark.kotlin.post
import spark.kotlin.put
import java.time.LocalDate


fun main() {

    val accountService = AccountService.getService(AccountRepositoryInMemory())

    get("/") { redirect("/docs") }

    get("/docs") { "API docs" }

    get("/account/:account") {
        val accountNumber = request.params(":account")

        accountService.getDetails(accountNumber)
                .mapLeft { NotFoundError("Account $accountNumber not found") }
                .toResponse(this)
    }

    get("/account/:account/balance") {
        val accountNumber = params(":account")
        accountService.balance(accountNumber)
                .mapLeft { NotFoundError("Account $accountNumber not found") }
                .toResponse(this)
    }

    put("/account") {
        Try { JsonAdaptors.fromJson<AccountOpenRequest>(request.body()) }
                .toEither { BadRequestError(it.message ?: "") }
                .flatMap { it.toEither { BadRequestError("Cannot parse request") } }
                .flatMap {
                    accountService.openAccount(it.holder, it.currency, LocalDate.now())
                            .mapLeft { AccountOpenError("Account cannot be opened") }

                }
                .toResponse(this)
    }

    post("/account/:account/deposit") {
        val accountNumber = params(":account")

        Try { JsonAdaptors.fromJson<DepositRequest>(request.body()) }
                .toEither { BadRequestError(it.message ?: "") }
                .flatMap { it.toEither { BadRequestError("Cannot parse request") } }
                .flatMap {
                    accountService.deposit(accountNumber, it.amount)
                            .mapLeft(::convertError)
                }
                .toResponse(this)
    }


    put("/transfer") {
        Try { JsonAdaptors.fromJson<TransferRequest>(request.body()) }
                .toEither { BadRequestError(it.message ?: "") }
                .flatMap { it.toEither { BadRequestError("Cannot parse request") } }
                .flatMap {
                    accountService.transfer(it).mapLeft(::convertError)
                }
                .toResponse(this)
    }

}


open class ErrorMessage(val code: Int, open val message: String)
data class BadRequestError(@Transient override val message: String) : ErrorMessage(400, message)
data class NotFoundError(@Transient override val message: String) : ErrorMessage(404, message)
data class AccountOpenError(@Transient override val message: String) : ErrorMessage(500, message)
data class GeneralError(@Transient override val message: String) : ErrorMessage(500, message)


private fun convertError(it: TransferError): ErrorMessage {
    return when (it) {
        is TransferError.RepositoryException -> GeneralError(it.toString())
        is TransferError.NegativeAmount -> BadRequestError("Cannot transfer negative amounts")
        is TransferError.NoSuchAccount -> BadRequestError("Account ${it.accountNumber} not found")
        is TransferError.DifferentCurrency -> BadRequestError("Currency mismatch")
        is TransferError.InsufficientFunds -> BadRequestError("Insufficient funds")
    }
}

inline fun <reified V> Either<ErrorMessage, V>.toResponse(routeHandler: RouteHandler): Any =
        this.fold(
                {
                    routeHandler.status(it.code);
                    JsonAdaptors.toJson(it)
                },
                JsonAdaptors::toJson)
