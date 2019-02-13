package com.mkorneev.money_transfers

import com.despegar.http.client.HttpResponse
import com.despegar.sparkjava.test.SparkServer
import com.mkorneev.money_transfers.model.*
import com.mkorneev.money_transfers.rest.JsonAdaptors
import com.mkorneev.money_transfers.rest.ServerKt
import org.javamoney.moneta.Money
import org.junit.ClassRule
import spark.servlet.SparkApplication
import spock.lang.Shared
import spock.lang.Specification

import javax.money.CurrencyUnit
import javax.money.Monetary

class ApiSpec extends Specification {
    public static final int port = 14567
    public static final CurrencyUnit EUR = Monetary.getCurrency("EUR")
    public static final Money EUR_10 = Money.of(10, EUR)

    static class ApiTestSparkApplication implements SparkApplication {
        void init() {
            ServerKt.main(port.toString())
        }
    }

    @ClassRule @Shared
    SparkServer testServer = new SparkServer<>(ApiTestSparkApplication.class, port)


    def "show doc page"() {
        when:
        def method = testServer.get("/", true)
        def response = testServer.execute(method)

        then:
        response.code() == 200

        def body = new String(response.body())
        body.contains("Please refer to API documentation at")
    }

    def "open an account with no data"() {
        when:
        def method = testServer.post("/api/accounts", "", false)
        def response = testServer.execute(method)

        then:
        assertError(400, "End of input", response)
    }

    def "open an account with insufficient data"() {
        when:
        def json = '{ "holder": { "fullName": null }, "currency": "EUR" }'
        def method = testServer.post("/api/accounts", json, false)
        def response = testServer.execute(method)

        then:
        assertError(400, "Non-null value 'fullName' was null at \$.holder.fullName", response)
    }

    def "open and get an account"() {
        when:
        def holder = new Holder("Client", "Somewhere")
        def accountRequest = new AccountOpenRequest(holder, EUR)
        def accountRequestJson = JsonAdaptors.accountOpenRequestAdaptor().toJson(accountRequest)

        def method = testServer.post("/api/accounts", accountRequestJson, false)
        def response = testServer.execute(method)

        then:
        response.code() == 200

        def body = new String(response.body())
        def account = JsonAdaptors.accountAdaptor().fromJson(body)

        account.number.length() == 16  // IBAN length

        and:
        def method2 = testServer.get("/api/accounts/$account.number", false)
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
        def method = testServer.get("/api/accounts/test", false)
        def response = testServer.execute(method)

        then:
        assertError(404, "Account test not found", response)
    }

    def "get account balance"() {
        given:
        def a = openAccount()

        when:
        def method = testServer.get("/api/accounts/$a.number/balance", false)
        def response = testServer.execute(method)

        then:
        response.code() == 200

        def body = new String(response.body())
        def amount = JsonAdaptors.moneyAdaptor().fromJson(body)
        amount == Money.of(0, EUR)
    }

    def "transfer if insufficient funds"() {
        given:
        def a = openAccount()
        def b = openAccount()

        expect:
        a.number != b.number

        when:
        def transferRequest = new TransferRequest(a.number, b.number, EUR_10)
        def transferRequestJson = JsonAdaptors.toJsonJava(transferRequest)

        def method = testServer.post("/api/transfer", transferRequestJson, false)
        def response = testServer.execute(method)

        then:
        assertError(400, "Insufficient funds", response)
    }

    def "deposit money into account"() {
        given:
        def a = openAccount()

        when:
        def depositRequest = new DepositRequest(a.number, EUR_10)
        def depositRequestJson = JsonAdaptors.toJsonJava(depositRequest)

        def method = testServer.post("/api/accounts/$a.number/deposit", depositRequestJson, false)
        def response = testServer.execute(method)

        then:
        response.code() == 200

        getBalance(a.number) == EUR_10
    }

    def "withdraw money from account"() {
        given:
        def a = openAccount()
        deposit(a.number, EUR_10)

        when:
        def withdrawRequest = new WithdrawRequest(a.number, EUR_10)
        def withdrawRequestJson = JsonAdaptors.toJsonJava(withdrawRequest)

        def method = testServer.post("/api/accounts/$a.number/withdraw", withdrawRequestJson, false)
        def response = testServer.execute(method)

        then:
        response.code() == 200

        getBalance(a.number).isZero()
    }

    def "transfer"() {
        given:
        def a = openAccount()
        def b = openAccount()
        deposit(a.number, EUR_10)

        when:
        def transferRequest = new TransferRequest(a.number, b.number, EUR_10)
        def transferRequestJson = JsonAdaptors.toJsonJava(transferRequest)

        def method = testServer.post("/api/transfer", transferRequestJson, false)
        def response = testServer.execute(method)

        then:
        response.code() == 200
        getBalance(a.number).isZero()
        getBalance(b.number) == EUR_10
    }


    private static void assertError(code, message, HttpResponse response) {
        assert response.code() == code

        def body = new String(response.body())
        def error = JsonAdaptors.errorAdaptor().fromJson(body)

        assert error.code == code
        assert error.message == message
    }


    Account openAccount() {
        def holder = new Holder("Client", "Somewhere")
        def accountRequest = new AccountOpenRequest(holder, EUR)
        def accountRequestJson = JsonAdaptors.accountOpenRequestAdaptor().toJson(accountRequest)

        def method = testServer.post("/api/accounts", accountRequestJson, false)
        def response = testServer.execute(method)

        def body = new String(response.body())
        def account = JsonAdaptors.accountAdaptor().fromJson(body)

        return account
    }

    Money getBalance(accountNumber) {
        def method = testServer.get("/api/accounts/$accountNumber/balance", false)
        def response = testServer.execute(method)

        def body = new String(response.body())
        def balance = JsonAdaptors.moneyAdaptor().fromJson(body)

        return balance
    }

    boolean deposit(accountNumber, amount) {
        def depositRequest = new DepositRequest(accountNumber, amount)
        def depositRequestJson = JsonAdaptors.toJsonJava(depositRequest)

        def method = testServer.post("/api/accounts/$accountNumber/deposit", depositRequestJson, false)
        def response = testServer.execute(method)

        response.code() == 200
    }

}