package basics

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializerProvider
import enums.Currency
import enums.ExchangeDirection
import java.io.IOException

data class CurrencyPair internal constructor(val baseCurrency: Currency,
                                             val targetCurrency: Currency) {

    /**
     * Получение строкового представления
     */
    override fun toString(): String {
        return String.format(CONVERT_PATTERN, baseCurrency.name, targetCurrency.name)
    }

    val direction: ExchangeDirection = when {
        baseCurrency.isCrypto and targetCurrency.isCrypto -> ExchangeDirection.CONVERT_CRYPTO
        baseCurrency.isCrypto and targetCurrency.isFiat -> ExchangeDirection.SELL_CRYPTO
        baseCurrency.isFiat and targetCurrency.isCrypto -> ExchangeDirection.BUY_CRYPTO
        else -> ExchangeDirection.CONVERT_FIAT
    }

    companion object {
        private const val CONVERT_PATTERN = "%s/%s"

        fun of(base: Currency, target: Currency): CurrencyPair {
            return CurrencyPair(base, target)
        }

        fun fromString(pairString: String): CurrencyPair {
            val currencies = pairString.split("/".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            if (currencies.size != 2) {
                throw IllegalArgumentException("Illegal currency pair value")
            }
            return CurrencyPair(
                Currency.valueOf(currencies[0].toUpperCase()),
                Currency.valueOf(currencies[1].toUpperCase())
            )
        }
    }
}