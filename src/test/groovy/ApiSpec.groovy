import com.despegar.sparkjava.test.SparkServer
import com.mkorneev.money_transfers.Account
import com.mkorneev.money_transfers.AccountOpenRequest
import com.mkorneev.money_transfers.DepositRequest
import com.mkorneev.money_transfers.Holder
import com.mkorneev.money_transfers.JsonAdaptors
import com.mkorneev.money_transfers.ServerKt
import com.mkorneev.money_transfers.TransferRequest
import org.javamoney.moneta.Money
import org.junit.ClassRule
import spark.servlet.SparkApplication
import spock.lang.Shared
import spock.lang.Specification

import javax.money.CurrencyUnit
import javax.money.Monetary

class ApiSpec extends Specification {
    static class ApiTestSparkApplication implements SparkApplication {
        void init() {
            ServerKt.main()
        }
    }

    @ClassRule
    @Shared
    SparkServer testServer = new SparkServer<>(ApiTestSparkApplication.class, 4567)

    public static final CurrencyUnit EUR = Monetary.getCurrency("EUR")

    def "show doc page"() {
        when:
        def method = testServer.get("/", true)
        def response = testServer.execute(method)

        then:
        response.code() == 200

        def body = new String(response.body())
        body == "API docs"
    }

    def "open an account with no data"() {
        when:
        def method = testServer.put("/account", "", false)
        def response = testServer.execute(method)

        then:
        response.code() == 400

        def body = new String(response.body())
        def error = JsonAdaptors.errorAdaptor().fromJson(body)

        error.code == 400
        error.message == "End of input"
    }

    def "open and get an account"() {
        when:
        def holder = new Holder("Client", "Somewhere")
        def accountRequest = new AccountOpenRequest(holder, EUR)
        def accountRequestJson = JsonAdaptors.accountOpenRequestAdaptor().toJson(accountRequest)

        def method = testServer.put("/account", accountRequestJson, false)
        def response = testServer.execute(method)

        then:
        response.code() == 200

        def body = new String(response.body())
        def account = JsonAdaptors.accountAdaptor().fromJson(body)

        account.number.length() == 16  // IBAN length

        and:
        def method2 = testServer.get("/account/$account.number", false)
        def response2 = testServer.execute(method2)

        response2.code() == 200

        def body2 = new String(response2.body())
        def account2 = JsonAdaptors.accountAdaptor().fromJson(body2)

        account2.number == account.number
        account2.holder == holder
        account2.currency == EUR
        account2.balance.number == 0
    }

    def "get non-existent account"() {
        when:
        def method = testServer.get("/account/test", false)
        def response = testServer.execute(method)

        then:
        response.code() == 404

        def body = new String(response.body())
        def error = JsonAdaptors.errorAdaptor().fromJson(body)

        error.code == 404
        error.message == "Account test not found"
    }

    def "get account balance"() {
        when:
        def method = testServer.get("/account/test/balance", false)
        def response = testServer.execute(method)

        then:
        response.code() == 404

        def body = new String(response.body())
        def error = JsonAdaptors.errorAdaptor().fromJson(body)

        error.code == 404
        error.message == "Account test not found"
    }

    def "transfer if insufficient funds"() {
        given:
        def a = openAccount()
        def b = openAccount()

        expect:
        a.number != b.number

        when:
        def transferRequest = new TransferRequest(a.number, b.number, Money.of(10, EUR))
        def transferRequestJson = JsonAdaptors.toJsonJava(transferRequest)

        def method = testServer.put("/transfer", transferRequestJson, false)
        def response = testServer.execute(method)

        then:
        response.code() == 400
        def body = new String(response.body())
        def error = JsonAdaptors.errorAdaptor().fromJson(body)

        error.code == 400
        error.message == "Insufficient funds"

    }

    def "deposit money in an account"() {
        given:
        def a = openAccount()

        when:
        def depositRequest = new DepositRequest(a.number, amount)
        def depositRequestJson = JsonAdaptors.toJsonJava(depositRequest)

        def method = testServer.post("/account/$a.number/deposit", depositRequestJson, false)
        def response = testServer.execute(method)

        then:
        response.code() == 200

        getBalance(a.number) == amount

        where:
        amount = Money.of(10, EUR)

    }

    def "transfer"() {
        given:
        def a = openAccount()
        def b = openAccount()
        deposit(a.number, amount)

        when:
        def transferRequest = new TransferRequest(a.number, b.number, Money.of(10, EUR))
        def transferRequestJson = JsonAdaptors.toJsonJava(transferRequest)

        def method = testServer.put("/transfer", transferRequestJson, false)
        def response = testServer.execute(method)

        then:
        response.code() == 200
        getBalance(a.number).isZero()
        getBalance(b.number) == amount

        where:
        amount = Money.of(10, EUR)

    }

    Account openAccount() {
        def holder = new Holder("Client", "Somewhere")
        def accountRequest = new AccountOpenRequest(holder, EUR)
        def accountRequestJson = JsonAdaptors.accountOpenRequestAdaptor().toJson(accountRequest)

        def method = testServer.put("/account", accountRequestJson, false)
        def response = testServer.execute(method)

        def body = new String(response.body())
        def account = JsonAdaptors.accountAdaptor().fromJson(body)

        return account
    }

    Money getBalance(accountNumber) {
        def method = testServer.get("/account/$accountNumber/balance", false)
        def response = testServer.execute(method)

        def body = new String(response.body())
        def balance = JsonAdaptors.moneyAdaptor().fromJson(body)

        return balance
    }

    boolean deposit(accountNumber, amount) {
        def depositRequest = new DepositRequest(accountNumber, amount)
        def depositRequestJson = JsonAdaptors.toJsonJava(depositRequest)

        def method = testServer.post("/account/$accountNumber/deposit", depositRequestJson, false)
        def response = testServer.execute(method)

        response.code() == 200
    }

}