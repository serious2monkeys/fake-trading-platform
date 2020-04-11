package ru.doronin.bootstrap

import com.github.javafaker.Faker
import enums.UserRole
import io.ktor.util.InternalAPI
import mu.KotlinLogging
import org.apache.commons.lang3.RandomStringUtils
import ru.doronin.database.MongoIntegrator
import ru.doronin.domains.PlatformUser
import java.security.SecureRandom

private val SECURE_RANDOM = SecureRandom()
private val FAKER_INSTANCE = Faker(SECURE_RANDOM)
private val logger = KotlinLogging.logger {}

@InternalAPI
fun runInitialBootstrapping() {
    val admin =
            PlatformUser(
                    login = "admin",
                    email = "admin@fake.platform",
                    password = "password",
                    role = UserRole.ADMIN
            )
    MongoIntegrator.saveAsync(admin)
    repeat(10) { generateUserData() }
}

@InternalAPI
fun generateUserData() {
    val user = PlatformUser(
            login = FAKER_INSTANCE.ancient().hero().replace(Regex("\\s+"), "_"),
            email = FAKER_INSTANCE.internet().emailAddress(),
            password = RandomStringUtils.randomAlphanumeric(16)
    )
    logger.info { "Saving user with login ${user.login} and password ${user.password}" }
    MongoIntegrator.saveAsync(user)
}
