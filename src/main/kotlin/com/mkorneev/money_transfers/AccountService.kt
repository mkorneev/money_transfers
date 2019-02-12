package com.mkorneev.money_transfers

import arrow.core.*
import arrow.core.extensions.either.monad.flatten
import com.mkorneev.money_transfers.TransferError.*
import org.iban4j.CountryCode
import org.iban4j.Iban
import org.javamoney.moneta.Money
import java.time.Instant
import java.time.LocalDate
import javax.money.CurrencyUnit
import arrow.core.extensions.`try`.monad.binding as bindingT
import arrow.core.extensions.either.monad.binding as bindingE


class AccountService private constructor(private val accountRepository: AccountRepository) : IAccountService {
    companion object : SingletonHolder<AccountService, AccountRepository>(::AccountService) {
        @JvmStatic
        fun getService(ar: AccountRepository) = AccountService.getInstance(ar)
    }


    private fun generateUniqueAccountNumber(): Try<AccountNumber> {
        val accountNumber: AccountNumber = Iban.random(CountryCode.BE).toString()
        return accountRepository.exists(accountNumber)
                .flatMap { exists ->
                    if (!exists) accountNumber.success()
                    else Exception("Account number is not unique, please try again").failure()
                }
    }


    override fun openAccount(holder: Holder, currency: CurrencyUnit, openedAt: LocalDate): Either<OpenError, Account> =
            generateUniqueAccountNumber()
                    .toEither { OpenError.RepositoryException(it) }
                    .flatMap { accountNumber ->
                        val account = Account(accountNumber, holder, openedAt, Option.empty(),
                                Money.of(0, currency))

                        accountRepository.store(account)
                                .toEither { OpenError.RepositoryException(it) }
                    }


    override fun withdraw(accountNumber: AccountNumber, amount: Money): Either<TransferError, Account> =
            accountRepository.query(accountNumber)
                    .toEither { RepositoryException(it) }
                    .flatMap {
                        it
                                .mapLeft { NoSuchAccount(accountNumber) }
                                .flatMap { account -> debit(account, amount) }
                    }

    private fun debit(account: Account, amount: Money): Either<TransferError, Account> =
            when {
                amount.isNegative -> NegativeAmount.left()
                account.currency != amount.currency -> DifferentCurrency(account.number).left()
                account.balance < amount -> InsufficientFunds(account.number).left()
                else -> {
                    val result = account.copy(balance = account.balance.subtract(amount))
                    accountRepository.store(result)
                            .toEither().mapLeft { RepositoryException(it) }
                }
            }


    override fun deposit(accountNumber: AccountNumber, amount: Money): Either<TransferError, Account> =
            accountRepository.query(accountNumber)
                    .toEither { RepositoryException(it) }
                    .flatMap {
                        it
                                .mapLeft { NoSuchAccount(accountNumber) }
                                .flatMap { account -> credit(account, amount) }
                    }

    private fun credit(account: Account, amount: Money): Either<TransferError, Account> =
            when {
                amount.isNegative -> NegativeAmount.left()
                account.currency != amount.currency -> DifferentCurrency(account.number).left()
                else -> {
                    val result = account.copy(balance = account.balance.add(amount))
                    accountRepository.store(result)
                            .toEither().mapLeft { RepositoryException(it) }
                }
            }


    override fun transfer(request: TransferRequest): Either<TransferError, AccountTransaction> {
        val amount = request.amount
        if (amount.isNegative) return NegativeAmount.left()

        return bindingT {
            val (fromEither) = accountRepository.query(request.from)
            val (toEither) = accountRepository.query(request.to)

            bindingE<TransferError, AccountTransaction> {
                val (from) = fromEither.mapLeft { NoSuchAccount(request.from) }
                val (to) = toEither.mapLeft { NoSuchAccount(request.to) }
                val (transaction) = transfer(from, to, amount, request.message)
                transaction
            }
        }
                .toEither { RepositoryException(it) }.flatten()
    }

    private fun transfer(from: Account, to: Account, amount: Money, message: String = "",
                         overdraftAllowed: Boolean = false): Either<TransferError, AccountTransaction> =
            when {
                from.currency != amount.currency -> DifferentCurrency(from.number).left()
                to.currency != amount.currency -> DifferentCurrency(to.number).left()
                !overdraftAllowed && from.balance < amount -> InsufficientFunds(from.number).left()
                else -> bindingE {
                    val (f) = debit(from, amount)
                    val (t) = credit(to, amount)
                    AccountTransaction("a", f.number, t.number, amount, Instant.now(), message)
                }
            }

    override fun getDetails(accountNumber: AccountNumber): Either<BalanceError, Account> =
            accountRepository.query(accountNumber)
                    .toEither { BalanceError.RepositoryException(it) }
                    .flatMap {
                        it
                                .mapLeft { BalanceError.NoSuchAccount }
                                .map { account -> account }
                    }

    override fun balance(accountNumber: AccountNumber): Either<BalanceError, Money> =
            getDetails(accountNumber).map { it.balance }

}