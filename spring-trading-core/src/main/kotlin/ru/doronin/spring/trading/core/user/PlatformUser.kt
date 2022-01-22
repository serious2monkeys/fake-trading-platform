package ru.doronin.spring.trading.core.user

import enums.UserRole
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.mongodb.core.mapping.Document
import java.time.Instant
import javax.validation.constraints.Email
import javax.validation.constraints.NotBlank

/**
 * Simple representation of user
 */
@Document
data class PlatformUser(
    @Id var id: String? = null,
    var login: @NotBlank String,
    var email: @Email String,
    var password: @NotBlank String,
    @CreatedDate var created: Instant = Instant.now(),
    @LastModifiedDate var modified: Instant = Instant.now(),
    var role: UserRole = UserRole.TRADER
)
