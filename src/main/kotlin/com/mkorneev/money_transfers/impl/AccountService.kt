package com.mkorneev.money_transfers.impl

import arrow.core.*
import arrow.core.extensions.either.monad.binding
import com.mkorneev.money_transfers.util.SingletonHolder
import com.mkorneev.money_transfers.model.*
import com.mkorneev.money_transfers.model.TransferError.*
import org.iban4j.CountryCode
import org.iban4j.Iban
import org.javamoney.moneta.Money
import java.lang.RuntimeException
import java.time.Instant
import java.time.LocalDate
import javax.money.CurrencyUnit

data class Repositories(val accountRepository: AccountRepository, val transactionRepository: TransactionRepository)

class AccountService private constructor(repositories: Repositories) : IAccountService {
    companion object : SingletonHolder<AccountService, Repositories>(::AccountService) {
        @JvmStatic
        fun getService(repositories: Repositories) = AccountService.getInstance(repositories)
    }

    private val accountRepository = repositories.accountRepository
    private val transactionRepository = repositories.transactionRepository

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

                        accountRepository.create(account.number, account)
                                .mapLeft { OpenError.DuplicateId }
                    }


    override fun withdraw(accountNumber: AccountNumber, amount: Money): Either<TransferError, Account> =
            accountRepository.transform1(accountNumber, debit(amount))
                    .mapLeft { it.fold({ NoSuchAccount(accountNumber) }, { e -> e }) }

    private fun debit(amount: Money): (Account) -> Either<TransferError, Account> = { account: Account ->
        when {
            amount.isNegative -> NegativeAmount.left()
            account.currency != amount.currency -> DifferentCurrency(account.number).left()
            account.balance < amount -> InsufficientFunds(account.number).left()
            else -> {
                account.copy(balance = account.balance.subtract(amount)).right()
            }
        }
    }

    override fun deposit(accountNumber: AccountNumber, amount: Money): Either<TransferError, Account> =
            accountRepository.transform1(accountNumber, credit(amount))
                    .mapLeft { it.fold({ NoSuchAccount(accountNumber) }, { e -> e }) }


    private fun credit(amount: Money): (Account) -> Either<TransferError, Account> = { account: Account ->
        when {
            amount.isNegative -> NegativeAmount.left()
            account.currency != amount.currency -> DifferentCurrency(account.number).left()
            else -> {
                account.copy(balance = account.balance.add(amount)).right()
            }
        }
    }


    override fun transfer(request: TransferRequest): Either<TransferError, AccountTransaction> {
        val amount = request.amount
        if (amount.isNegative) return NegativeAmount.left()

        val updates: List<Update<TransferError>> = transfer(request.from, request.to, amount)

        return accountRepository.transform(updates)
                .mapLeft { it.fold({ notFound -> NoSuchAccount(notFound.id) }, { e -> e }) }
                .flatMap { storeTransaction(request) }
    }

    // Use only for testing
    fun transferNotAtomic(request: TransferRequest): Either<TransferError, AccountTransaction> {
        val amount = request.amount
        if (amount.isNegative) return NegativeAmount.left()

        return binding {
            val (w) = withdraw(request.from, amount)
            val (d) = deposit(request.to, amount)
            AccountTransaction(getUniqueTransactionId(), w.number, d.number,
                    amount, Instant.now(), request.message)
        }
    }

    private fun transfer(from: AccountNumber, to: AccountNumber, amount: Money,
                         overdraftAllowed: Boolean = false): List<Update<TransferError>> {
        val d = Update(from) { a ->
            when {
                a.currency != amount.currency -> DifferentCurrency(a.number).left()
                !overdraftAllowed && a.balance < amount -> InsufficientFunds(a.number).left()
                else -> debit(amount)(a)
            }
        }

        val c = Update(to) { a ->
            when {
                a.currency != amount.currency -> DifferentCurrency(a.number).left()
                else -> credit(amount)(a)
            }
        }

        return listOf(d, c)
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


    private fun storeTransaction(request: TransferRequest): Either<RepositoryException, AccountTransaction> {
        val transaction = AccountTransaction(getUniqueTransactionId(), request.from, request.to,
                request.amount, Instant.now(), request.message)
        return transactionRepository.create(transaction.id, transaction)
                .mapLeft { RepositoryException(RuntimeException("Duplicate transaction ID")) }
    }

}