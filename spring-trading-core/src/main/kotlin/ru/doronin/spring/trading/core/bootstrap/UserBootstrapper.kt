package ru.doronin.spring.trading.core.bootstrap

import com.github.javafaker.Faker
import mu.KotlinLogging
import org.apache.commons.lang3.RandomStringUtils
import org.springframework.context.ApplicationListener
import org.springframework.context.event.ContextRefreshedEvent
import org.springframework.stereotype.Component
import ru.doronin.spring.trading.core.user.PlatformUser
import ru.doronin.spring.trading.core.user.UserRole
import ru.doronin.spring.trading.core.user.UserService
import java.security.SecureRandom

private val logger = KotlinLogging.logger {}
private val SECURE_RANDOM = SecureRandom()
private val FAKER_INSTANCE = Faker(SECURE_RANDOM)

@Component
class UserBootstrapper(private val userService: UserService) : ApplicationListener<ContextRefreshedEvent> {
    private val logger = KotlinLogging.logger {}

    override fun onApplicationEvent(event: ContextRefreshedEvent) {
        val admin =
            PlatformUser(
                login = "admin",
                email = "admin@fake.platform",
                password = "password",
                role = UserRole.ADMIN
            )
        userService.save(admin).subscribe { logger.info("Admin successfully saved") }
        repeat(10) { generateFakeUser() }
    }

    private fun generateFakeUser() {
        val user = PlatformUser(
            login = FAKER_INSTANCE.ancient().god().replace(Regex("\\s+"), "_"),
            email = FAKER_INSTANCE.internet().safeEmailAddress(),
            password = RandomStringUtils.randomAlphanumeric(16)
        )
        logger.info { "Saving user with login ${user.login} and password ${user.password}" }
        userService.save(user).subscribe { savedUser -> logger.info("${savedUser.login} successfully saved") }
    }
}