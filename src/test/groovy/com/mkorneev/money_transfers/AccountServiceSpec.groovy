package com.mkorneev.money_transfers

import arrow.core.Either
import com.mkorneev.money_transfers.impl.AccountRepositoryInMemory
import com.mkorneev.money_transfers.impl.AccountService
import com.mkorneev.money_transfers.impl.Repositories
import com.mkorneev.money_transfers.impl.TransactionRepositoryInMemory
import com.mkorneev.money_transfers.model.Holder
import com.mkorneev.money_transfers.model.TransferError
import com.mkorneev.money_transfers.model.TransferRequest
import org.javamoney.moneta.Money
import spock.lang.Specification

import javax.money.CurrencyUnit
import javax.money.Monetary
import java.time.Clock
import java.time.LocalDate

class AccountServiceSpec extends Specification {

    public static final CurrencyUnit EUR = Monetary.getCurrency("EUR")
    public static final CurrencyUnit GBP = Monetary.getCurrency("GBP")
    public static final holder = new Holder("Client", "Somewhere")
    public static final clock = Clock.systemUTC()
    public static final today = LocalDate.now(clock)
    public static final Money EUR_10 = Money.of(10, "EUR")

    AccountService accountService = AccountService.getService(
            new Repositories(new AccountRepositoryInMemory(), new TransactionRepositoryInMemory()))

    def "open an account"() {
        when:
        def account = accountService.openAccount(holder, EUR, today).b

        then:
        account.balance.isZero()
        account.holder == holder
        account.currency == EUR
        account.openedAt == today
        account.number.length() == 16  // IBAN length

        accountService.balance(account.number) == new Either.Right(Money.of(0, "EUR"))
    }


    def "withdraw and deposit"() {
        given:
        def a = accountService.openAccount(holder, EUR, today).b

        expect:
        accountService.withdraw(a.number, EUR_10) ==
                new Either.Left(new TransferError.InsufficientFunds(a.number))

        accountService.deposit(a.number, Money.of(100, "EUR")).right
        accountService.withdraw(a.number, EUR_10).right

        accountService.balance(a.number) == new Either.Right(Money.of(90, "EUR"))

        // should not allow deposits and withdrawals in a different currency
        accountService.deposit(a.number, Money.of(100, "GBP")).left
        accountService.withdraw(a.number, Money.of(10, "GBP")).left
    }


    def "withdraw and deposit errors"() {
        given:
        def a = accountService.openAccount(holder, EUR, today).b

        expect:
        accountService.withdraw(notAccountNumber, EUR_10) ==
                new Either.Left(new TransferError.NoSuchAccount(notAccountNumber))

        accountService.deposit(notAccountNumber, EUR_10) ==
                new Either.Left(new TransferError.NoSuchAccount(notAccountNumber))

        accountService.withdraw(a.number, Money.of(-10, "EUR")).left
        accountService.deposit(a.number, Money.of(-10, "EUR")).left

        where:
        notAccountNumber = "not an account number"
    }


    def "transfer"() {
        given:
        def a = accountService.openAccount(holder, EUR, today).b
        def b = accountService.openAccount(holder, EUR, today).b
        def transferRequest = new TransferRequest(a.number, b.number, EUR_10, "To my friend")
        accountService.deposit(a.number, Money.of(100, "EUR"))

        when:
        def transaction = accountService.transfer(transferRequest).b

        then:
        with(transaction) {
            id.length() == 36  // UUID with dashes
            from == a.number
            to == b.number
            amount.number == 10
            amount.currency == EUR
            message == "To my friend"
        }

        accountService.balance(a.number) == new Either.Right(Money.of(90, "EUR"))
        accountService.balance(b.number) == new Either.Right(EUR_10)
    }


    def "transfer with insufficient funds"() {
        given:
        def a = accountService.openAccount(holder, EUR, today).b
        def b = accountService.openAccount(holder, EUR, today).b

        expect:
        def result = accountService.transfer(new TransferRequest(a.number, b.number, EUR_10))

        result == new Either.Left(new TransferError.InsufficientFunds(a.number))

        accountService.balance(a.number) == new Either.Right(Money.of(0, "EUR"))
        accountService.balance(b.number) == new Either.Right(Money.of(0, "EUR"))
    }


    def "transfer with wrong currency"() {
        given:
        def a = accountService.openAccount(holder, EUR, today).b
        def b = accountService.openAccount(holder, EUR, today).b

        expect:
        accountService.deposit(a.number, Money.of(100, "GBP")) ==
                new Either.Left(new TransferError.DifferentCurrency(a.number))

        accountService.transfer(new TransferRequest(a.number, b.number, Money.of(10, "GBP"))) ==
                new Either.Left(new TransferError.DifferentCurrency(a.number))

        accountService.balance(a.number) == new Either.Right(Money.of(0, "EUR"))
        accountService.balance(b.number) == new Either.Right(Money.of(0, "EUR"))
    }


    def "transfer is atomic"() {
        given:
        def a = accountService.openAccount(holder, EUR, today).b
        def b = accountService.openAccount(holder, GBP, today).b  // Note a different currency here
        def c = accountService.openAccount(holder, EUR, today).b

        accountService.deposit(a.number, Money.of(100, "EUR"))

        def tr1 = new TransferRequest(a.number, b.number, Money.of(80, "EUR"))
        def tr2 = new TransferRequest(a.number, c.number, Money.of(70, "EUR"))

        expect:
        accountService.transfer(tr1).left   // Fails on different currency check
        accountService.transfer(tr2).right  // Will fail if the first part of the previous transaction was carried out

        accountService.balance(a.number) == new Either.Right(Money.of(30, "EUR"))
        accountService.balance(b.number) == new Either.Right(Money.of(0, "GBP"))
        accountService.balance(c.number) == new Either.Right(Money.of(70, "EUR"))
    }


    def "transferNotAtomic is not atomic"() {
        given:
        def a = accountService.openAccount(holder, EUR, today).b
        def b = accountService.openAccount(holder, GBP, today).b  // Note a different currency here
        def c = accountService.openAccount(holder, EUR, today).b

        accountService.deposit(a.number, Money.of(100, "EUR"))

        def tr1 = new TransferRequest(a.number, b.number, Money.of(80, "EUR"))
        def tr2 = new TransferRequest(a.number, c.number, Money.of(70, "EUR"))

        expect:
        accountService.transferNotAtomic(tr1).left   // Fails on different currency check
        accountService.transferNotAtomic(tr2).left   // Fails because the first part of the previous transaction was carried out

        accountService.balance(a.number) == new Either.Right(Money.of(20, "EUR"))
        accountService.balance(b.number) == new Either.Right(Money.of(0, "GBP"))
        accountService.balance(c.number) == new Either.Right(Money.of(0, "EUR"))
    }
}