package platform.database

import enums.UserRole
import java.time.Instant

data class PlatformUser(
  var id: String? = null,
  var login: String,
  var email: String,
  var password: String,
  var created: Instant = Instant.now(),
  var modified: Instant = Instant.now(),
  var role: UserRole = UserRole.TRADER
)
