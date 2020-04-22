package messaging

import basics.CurrencyPair
import enums.CryptoExchange
import enums.Currency

/**
 * Generic socket message content
 */
sealed class SocketMessage<out T>(
    open val timestamp: Long,
    open val type: MessageType,
    open val exchange: CryptoExchange,
    open val payload: T
)

/**
 * Message content for price update
 */
data class PriceMessage(
    override val timestamp: Long,
    override val exchange: CryptoExchange,
    override val payload: ConversionData
) : SocketMessage<ConversionData>(
    timestamp,
    MessageType.PRICE_UPDATE,
    exchange,
    payload
)

data class ConversionData(val pair: CurrencyPair, val rate: Number)

/**
 * Message content for exchange trade
 */
data class TradeMessage(
    override val timestamp: Long,
    override val exchange: CryptoExchange,
    override val payload: TradeData
) : SocketMessage<TradeData>(
    timestamp,
    MessageType.TRADE,
    exchange,
    payload
)

/**
 * Message content for user wallet info
 */
data class WalletStateMessage(
    override val timestamp: Long,
    override val exchange: CryptoExchange,
    override val payload: WalletState
) : SocketMessage<WalletState>(
    timestamp,
    MessageType.WALLET_STATE,
    exchange,
    payload
)


data class TradeData(
    val price: Number,
    val volume: Number,
    val pair: CurrencyPair
)

data class WalletState(
    val currency: Currency,
    val balance: Number
)

enum class MessageType {
    PRICE_UPDATE,
    TRADE,
    WALLET_STATE
}