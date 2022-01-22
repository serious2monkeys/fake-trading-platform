package ru.doronin.spring.trading.core.candles

import basics.CurrencyPair
import enums.CryptoExchange
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.Instant

@Service
class CandleService(private val candleRepository: CandleRepository) {

    @Transactional
    fun save(candle: ExchangeCandle): Mono<ExchangeCandle> = candleRepository.save(candle)

    @Transactional
    fun findRecent(exchange: CryptoExchange, pair: CurrencyPair): Mono<ExchangeCandle> =
        candleRepository.findDistinctFirstByExchangeAndPairOrderByTimestampDesc(exchange, pair)

    @Transactional
    fun loadAllForPeriod(start: Instant, end: Instant): Flux<ExchangeCandle> =
        candleRepository.findAllByTimestampBetweenOrderByTimestampAsc(start, end)
}
