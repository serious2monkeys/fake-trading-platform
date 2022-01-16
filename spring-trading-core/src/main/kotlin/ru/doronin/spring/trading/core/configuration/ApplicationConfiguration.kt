package ru.doronin.spring.trading.core.configuration

import basics.CurrencyPair
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.*
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import org.springframework.boot.jackson.JsonComponent
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration


/**
 * General-purpose configurations
 */
@Configuration
class ApplicationConfiguration {

    /**
     * Configuration of JSON <-> POJO mapper
     */
    @Bean
    fun mapper(): ObjectMapper {
        val mapper = ObjectMapper().registerKotlinModule()
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL)
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        mapper.configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true)
        mapper.configure(JsonParser.Feature.IGNORE_UNDEFINED, true)
        mapper.registerModule(JavaTimeModule())
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        return mapper
    }

    @JsonComponent
    class PairSerializer : JsonSerializer<CurrencyPair>() {
        override fun serialize(value: CurrencyPair, gen: JsonGenerator, serializers: SerializerProvider) =
            gen.writeString(value.toString())
    }

    @JsonComponent
    class PairDeserializer : JsonDeserializer<CurrencyPair>() {
        override fun deserialize(p: JsonParser, ctxt: DeserializationContext): CurrencyPair =
            CurrencyPair.fromString(p.valueAsString)
    }
}
