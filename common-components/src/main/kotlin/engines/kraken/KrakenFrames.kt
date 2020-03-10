package engines.kraken

import basics.CurrencyPair
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ArrayNode
import enums.CryptoExchange
import enums.Currency
import messaging.ConversionData
import messaging.PriceMessage
import java.math.BigDecimal
import java.util.function.Predicate

const val FEED_ADDRESS = "wss://ws.kraken.com"

private val currencyCodes = mapOf(
    "XBT" to Currency.BTC,
    "ETH" to Currency.ETH,
    "EUR" to Currency.EUR,
    "USD" to Currency.USD
)

/**
 * Predicate for checking raw socket trade message
 */
val isMessageValid = Predicate<JsonNode> { node ->
    if (node is ArrayNode) {
        if (node.size() == 4) {
            if (node[0].isInt
                and node[1].isArray
                and node[2].isTextual
                and node[3].isTextual
            ) {
                if (node[2].textValue() == "trade") {
                    val pairComponents = node[3].textValue().split(Regex.fromLiteral("/"))
                    if ((pairComponents.size == 2)
                        and pairComponents.all { currencyCodes.containsKey(it) }
                    ) {
                        val tradesData = node[1] as ArrayNode
                        tradesData.all { tradeDataNode ->
                            (tradeDataNode is ArrayNode) and (0 to 2).toList().all { tradeDataNode[it].textValue().toBigDecimalOrNull() != null }
                        }
                    } else false
                } else false
            } else false
        } else false
    } else false
}

data class KrakenSubscriptionInfo(
    val name: String = "trade",
    val interval: Int? = null
)

data class KrakenSubscriptionStatus(
    var channelID: Int = 0,
    var event: String = "subscriptionStatus",
    var pair: String = "",
    var status: String = "",
    var subscription: KrakenSubscriptionInfo = KrakenSubscriptionInfo()
)

data class KrakenSocketEvent(
    val event: String = "subscribe",
    var pair: List<String> = emptyList(),
    var subscription: KrakenSubscriptionInfo = KrakenSubscriptionInfo()
)

fun convertToPriceMessages(rawMessage: ArrayNode): List<PriceMessage> {
    val tradesData = rawMessage[1] as ArrayNode
    val (baseCode, targetCode) = rawMessage[3].textValue().split(Regex.fromLiteral("/"))
    val pair = CurrencyPair.of(
        currencyCodes[baseCode] ?: error("Wrong base currency code"),
        currencyCodes[targetCode] ?: error("Wrong target currency code")
    )
    val thousand = BigDecimal("1000")
    return tradesData.asSequence().map { it as ArrayNode }
        .map { tradeData ->
            PriceMessage(
                timestamp = (tradeData[2].textValue().toBigDecimal() * thousand).toLong(),
                payload = ConversionData(
                    pair = pair,
                    rate = tradeData[0].textValue().toBigDecimal()
                ),
                exchange = CryptoExchange.KRAKEN
            )
        }.toList()
}