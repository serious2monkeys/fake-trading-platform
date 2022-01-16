package ru.doronin.spring.trading.core.exchange

import messaging.PriceMessage
import ru.doronin.spring.trading.core.candles.ExchangeCandle
import java.time.Instant

fun PriceMessage.toCandle(): ExchangeCandle = ExchangeCandle(
    timestamp = Instant.ofEpochMilli(this.timestamp),
    exchange = this.exchange,
    pair = this.payload.pair,
    rate = this.payload.rate.toFloat().toBigDecimal()
)

