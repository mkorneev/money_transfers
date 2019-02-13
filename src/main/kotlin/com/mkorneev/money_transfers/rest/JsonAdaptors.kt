package com.mkorneev.money_transfers.rest

import arrow.core.Option
import arrow.core.getOrElse
import arrow.core.some
import arrow.core.toOption
import com.mkorneev.money_transfers.model.*
import com.squareup.moshi.FromJson
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.ToJson
import org.javamoney.moneta.Money
import java.time.Instant
import java.time.LocalDate
import javax.money.CurrencyUnit
import javax.money.Monetary
import java.math.BigDecimal
import java.math.RoundingMode


class CurrencyUnitJsonAdaptor {
    @ToJson
    fun toJson(currencyUnit: CurrencyUnit): String = currencyUnit.currencyCode

    @FromJson
    fun fromJson(currency: String): CurrencyUnit = Monetary.getCurrency(currency)
}

class LocalDateJsonAdaptor {
    @ToJson
    fun toJson(date: LocalDate): String = date.toString()

    @FromJson
    fun fromJson(date: String): LocalDate = LocalDate.parse(date)
}

class InstantJsonAdaptor {
    @ToJson
    fun toJson(instant: Instant): Long = instant.toEpochMilli()

    @FromJson
    fun fromJson(instant: Long): Instant = Instant.ofEpochMilli(instant)
}

class OptionLocalDateJsonAdaptor {
    @ToJson
    fun toJson(date: Option<LocalDate>): String? = date.map { it.toString() }.getOrElse { "" }

    @FromJson
    fun fromJson(date: String?): Option<LocalDate> =
            if (date.isNullOrBlank()) Option.empty()
            else LocalDate.parse(date).some()
}

class MoneyJsonAdaptor {
    data class MoneyJson(val amount: String, val currency: CurrencyUnit)

    @ToJson
    fun toJson(value: Money): MoneyJson {
        val decimal = value.number.numberValueExact(BigDecimal::class.java)
        val defaultFractionDigits = value.currency.getDefaultFractionDigits()
        val scale = Math.max(decimal.scale(), defaultFractionDigits)

        val amount = decimal.setScale(scale, RoundingMode.UNNECESSARY).toString()

        return MoneyJson(amount, value.currency)
    }

    @FromJson
    fun fromJson(value: MoneyJson): Money = Money.of(BigDecimal(value.amount), value.currency)
}

object JsonAdaptors {
    val moshi = Moshi.Builder()
            .add(CurrencyUnitJsonAdaptor())
            .add(InstantJsonAdaptor())
            .add(LocalDateJsonAdaptor())
            .add(OptionLocalDateJsonAdaptor())
            .add(MoneyJsonAdaptor())
            .build()

    @JvmStatic
    fun accountAdaptor(): JsonAdapter<Account> {
        return moshi.adapter<Account>(Account::class.java)
    }

    @JvmStatic
    fun moneyAdaptor(): JsonAdapter<Money> {
        return moshi.adapter<Money>(Money::class.java)
    }

    @JvmStatic
    fun errorAdaptor(): JsonAdapter<ErrorMessage> {
        return moshi.adapter<ErrorMessage>(ErrorMessage::class.java)
    }

    @JvmStatic
    fun accountOpenRequestAdaptor(): JsonAdapter<AccountOpenRequest> {
        return moshi.adapter<AccountOpenRequest>(AccountOpenRequest::class.java)
    }

    @JvmStatic
    fun toJsonJava(value: TransferRequest) =
            moshi.adapter<TransferRequest>(TransferRequest::class.java).toJson(value)

    @JvmStatic
    fun toJsonJava(value: DepositRequest) =
            moshi.adapter<DepositRequest>(DepositRequest::class.java).toJson(value)

    @JvmStatic
    fun toJsonJava(value: WithdrawRequest) =
            moshi.adapter<WithdrawRequest>(WithdrawRequest::class.java).toJson(value)

    inline fun <reified V> toJson(value: V) =
            moshi.adapter<V>(V::class.java).toJson(value)

    inline fun <reified V> fromJson(value: String): Option<V> =
            moshi.adapter<V>(V::class.java).fromJson(value).toOption()

}