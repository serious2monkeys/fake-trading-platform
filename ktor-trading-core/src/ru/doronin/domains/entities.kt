package ru.doronin.domains

import enums.UserRole
import org.bson.codecs.pojo.annotations.BsonId
import org.litote.kmongo.Data
import org.litote.kmongo.Id
import org.litote.kmongo.newId
import java.time.Instant

@Data
data class PlatformUser(
        @BsonId var id: Id<PlatformUser> = newId(),
        var login: String,
        var email: String,
        var password: String,
        var created: Instant = Instant.now(),
        var modified: Instant = Instant.now(),
        var role: UserRole = UserRole.TRADER
)