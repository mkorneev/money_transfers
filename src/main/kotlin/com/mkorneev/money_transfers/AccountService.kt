package com.mkorneev.money_transfers

import arrow.core.*
import com.mkorneev.money_transfers.TransferError.*
import org.iban4j.CountryCode
import org.iban4j.Iban
import org.javamoney.moneta.Money
import java.time.Instant
import java.time.LocalDate
import javax.money.CurrencyUnit


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

                        accountRepository.create(account.number, account)
                                .mapLeft { OpenError.DuplicateId }
                    }


    override fun withdraw(accountNumber: AccountNumber, amount: Money): Either<TransferError, Account> =
            accountRepository.transform1(accountNumber, debit(amount))
                    .mapLeft { it.fold({ TransferError.NoSuchAccount(accountNumber) }, { e -> e }) }

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
                    .mapLeft { it.fold({ TransferError.NoSuchAccount(accountNumber) }, { e -> e }) }


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
                .mapLeft { it.fold({ notFound -> TransferError.NoSuchAccount(notFound.id) }, { e -> e }) }
                .map { AccountTransaction("a", request.from, request.to, amount, Instant.now(), request.message) }
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

}