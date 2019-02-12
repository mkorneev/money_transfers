import arrow.core.Either
import com.mkorneev.money_transfers.AccountRepositoryInMemory
import com.mkorneev.money_transfers.AccountService
import com.mkorneev.money_transfers.Holder
import com.mkorneev.money_transfers.TransferError
import com.mkorneev.money_transfers.TransferRequest
import org.javamoney.moneta.Money
import spock.lang.Ignore
import spock.lang.Specification

import javax.money.CurrencyUnit
import javax.money.Monetary
import java.time.Clock
import java.time.LocalDate

class AccountServiceSpec extends Specification {

    public static final CurrencyUnit EUR = Monetary.getCurrency("EUR")
    public static final holder = new Holder("Client", "Somewhere")
    public static final clock = Clock.systemUTC()
    public static final today = LocalDate.now(clock)

    AccountService accountService = AccountService.getService(new AccountRepositoryInMemory())

    def "open an account"() {
        when:
        def account = accountService.openAccount(holder, EUR, today).b

        then:
        account.balance.isZero()
        account.holder == holder
        account.currency == EUR
        account.openedAt == today
        account.number.length() == 19  // Formatted IBAN length

        accountService.balance(account.number) == new Either.Right(Money.of(0, "EUR"))
    }


    def "withdraw and deposit"() {
        given:
        def a = accountService.openAccount(holder, EUR, today).b

        expect:
        accountService.withdraw(a.number, Money.of(10, "EUR")) ==
                new Either.Left(new TransferError.InsufficientFunds(a.number))

        accountService.deposit(a.number, Money.of(100, "EUR")).right
        accountService.withdraw(a.number, Money.of(10, "EUR")).right

        accountService.balance(a.number) == new Either.Right(Money.of(90, "EUR"))

        // should not allow deposits and withdrawals in a different currency
        accountService.deposit(a.number, Money.of(100, "GBP")).left
        accountService.withdraw(a.number, Money.of(10, "GBP")).left
    }

    def "transfer"() {
        given:
        def a = accountService.openAccount(holder, EUR, today).b
        def b = accountService.openAccount(holder, EUR, today).b

        expect:
        accountService.deposit(a.number, Money.of(100, "EUR")).right
        accountService.transfer(new TransferRequest(a.number, b.number, Money.of(10, "EUR"))).right

        accountService.balance(a.number) == new Either.Right(Money.of(90, "EUR"))
        accountService.balance(b.number) == new Either.Right(Money.of(10, "EUR"))
    }


    def "transfer with insufficient funds"() {
        given:
        def a = accountService.openAccount(holder, EUR, today).b
        def b = accountService.openAccount(holder, EUR, today).b

        expect:
        def result = accountService.transfer(new TransferRequest(a.number, b.number, Money.of(10, "EUR")))

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


    @Ignore
    def "transfer race condition"() {
        given:
        def a = accountService.openAccount(holder, EUR, today).b
        def b = accountService.openAccount(holder, EUR, today).b

        expect:
        accountService.deposit(a.number, Money.of(100, "EUR")).right
        accountService.transfer(new TransferRequest(a.number, b.number, Money.of(10, "EUR"))).right
        accountService.transfer(new TransferRequest(b.number, a.number, Money.of(5, "EUR"))).right

        accountService.balance(a.number) == new Either.Right(Money.of(90, "EUR"))
        accountService.balance(b.number) == new Either.Right(Money.of(10, "EUR"))
    }
}