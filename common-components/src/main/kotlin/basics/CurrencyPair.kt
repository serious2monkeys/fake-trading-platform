package basics

import enums.Currency

data class CurrencyPair internal constructor(val baseCurrency: Currency,
                                             val targetCurrency: Currency) {

    /**
     * Получение строкового представления
     */
    override fun toString(): String {
        return String.format(CONVERT_PATTERN, baseCurrency.name, targetCurrency.name)
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
                    Currency.valueOf(currencies[0].uppercase()),
                    Currency.valueOf(currencies[1].uppercase())
            )
        }
    }
}
