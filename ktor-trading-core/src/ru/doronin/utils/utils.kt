package ru.doronin.utils

import at.favre.lib.crypto.bcrypt.BCrypt
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.MapperFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.ktor.util.InternalAPI
import io.ktor.util.toCharArray

private val hasher = BCrypt.withDefaults()
private val verifier = BCrypt.verifyer()

@InternalAPI
fun encodePassword(password: String): String = hasher.hashToString(12, password.toCharArray())

@InternalAPI
fun verifyPassword(password: String, encodedPassword: String): Boolean = verifier.verify(password.toCharArray(), encodedPassword).verified

object JsonMapper {
    // automatically installs the Kotlin module
    val defaultMapper: ObjectMapper = jacksonObjectMapper()

    init {
        with(defaultMapper) {
            enable(SerializationFeature.INDENT_OUTPUT)
            setSerializationInclusion(JsonInclude.Include.NON_NULL)
            configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true)
            configure(JsonParser.Feature.IGNORE_UNDEFINED, true)
            registerModule(JavaTimeModule())
            disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        }
    }
}