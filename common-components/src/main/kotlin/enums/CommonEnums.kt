package enums

/**
 * Simple enumeration of well-known currencies
 */
enum class Currency(private val type: CurrencyType) {
    EUR(CurrencyType.FIAT),
    USD(CurrencyType.FIAT),
    BTC(CurrencyType.CRYPTO),
    ETH(CurrencyType.CRYPTO);

    val isCrypto = type == CurrencyType.CRYPTO

    val isFiat = type == CurrencyType.FIAT
}

enum class CurrencyType {
    CRYPTO,
    FIAT
}

enum class UserRole {
    TRADER,
    ADMIN
}

enum class ExchangeDirection {
    BUY_CRYPTO,
    SELL_CRYPTO,
    CONVERT_CRYPTO,
    CONVERT_FIAT
}

enum class CryptoExchange {
    KRAKEN,
    COINBASE,
    FAKE_LOCAL
}