package ru.doronin.spring.trading.core.candles

import basics.CurrencyPair
import enums.CryptoExchange
import org.springframework.data.mongodb.repository.ReactiveMongoRepository
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.Instant

@Repository
interface CandleRepository : ReactiveMongoRepository<ExchangeCandle, String> {
    fun findDistinctFirstByExchangeAndPairOrderByTimestampDesc(
        exchange: CryptoExchange,
        pair: CurrencyPair
    ): Mono<ExchangeCandle>

    fun findAllByTimestampBetweenOrderByTimestampAsc(start: Instant, end: Instant): Flux<ExchangeCandle>
}
