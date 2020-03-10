package engines.coinbase

import basics.CurrencyPair
import com.fasterxml.jackson.databind.JsonNode
import enums.CryptoExchange
import enums.Currency
import messaging.ConversionData
import messaging.PriceMessage
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.function.Predicate
import kotlin.reflect.full.memberProperties

const val FEED_ADDRESS = "wss://ws-feed.pro.coinbase.com"

/**
 * Predicate for checking raw socket message
 */
val isCoinbaseTick = Predicate<JsonNode> { node ->
    if (node.isObject) {
        val properties = CoinbaseTick::class.memberProperties.map { it.name }
        properties.all { property -> node.hasNonNull(property) }
    } else {
        false
    }
}

/*
* Registry for conversion exchange products to common currency pairs
*/
private val productsMap = mapOf(
    "ETH-USD" to CurrencyPair.of(Currency.ETH, Currency.USD),
    "ETH-EUR" to CurrencyPair.of(Currency.ETH, Currency.EUR),
    "BTC-USD" to CurrencyPair.of(Currency.BTC, Currency.USD),
    "BTC-EUR" to CurrencyPair.of(Currency.BTC, Currency.EUR)
)

data class SubscriptionMessage(
    var type: String = "subscribe",
    var product_ids: Set<String> = emptySet(),
    var channels: Set<String> = emptySet(),
    var key: String? = null,
    var passphrase: String? = null,
    var timestamp: String = Instant.now().epochSecond.toString()
) {
    constructor(
        type: String = "subscribe",
        product_ids: Set<String> = emptySet(),
        channels: Set<String> = emptySet(),
        credentials: CoinbaseProCredentials,
        timestamp: String = Instant.now().epochSecond.toString()
    ) : this(
        type = type,
        product_ids = product_ids,
        channels = channels,
        key = credentials.apiKey,
        passphrase = credentials.passphrase,
        timestamp = timestamp
    )
}

/**
 * Wrapper for Coinbase generated ticker message content
 */
data class CoinbaseTick(
    var best_ask: Number,
    var best_bid: Float,
    var high_24h: Float,
    var last_size: Float,
    var low_24h: Float,
    var open_24h: Float,
    var price: Float,
    var product_id: String,
    var sequence: Long,
    var side: String,
    var time: LocalDateTime,
    var trade_id: Long,
    var type: String,
    var volume_24h: Float,
    var volume_30d: Float
) {
    fun toPriceMessage() = PriceMessage(
        timestamp = time.toInstant(ZoneOffset.UTC).toEpochMilli(),
        exchange = CryptoExchange.COINBASE,
        payload = ConversionData(pair = productsMap[product_id] ?: error("Wrong product"), rate = price)
    )

}

data class CoinbaseProCredentials(var apiKey: String, var passphrase: String, var secret: String)

fun generateSubscriptionMessage(credentials: CoinbaseProCredentials): SubscriptionMessage {
    return SubscriptionMessage(
        product_ids = productsMap.keys,
        channels = setOf("ticker"),
        key = credentials.apiKey,
        passphrase = credentials.passphrase,
        timestamp = Instant.now().epochSecond.toString()
    )
}