package ru.doronin.spring.trading.core.candles

import basics.CurrencyPair
import enums.CryptoExchange
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import java.math.BigDecimal
import java.time.Instant

/**
 * Simple representation of exchange candle
 */
@Document
data class ExchangeCandle(
    @Id var id: String? = null,
    val timestamp: Instant,
    val exchange: CryptoExchange,
    val pair: CurrencyPair,
    val rate: BigDecimal
)
