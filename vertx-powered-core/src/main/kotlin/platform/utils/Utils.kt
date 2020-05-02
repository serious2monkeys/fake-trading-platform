package platform.utils

import at.favre.lib.crypto.bcrypt.BCrypt

private val hasher = BCrypt.withDefaults()
private val verifier = BCrypt.verifyer()

fun encodePassword(password: String): String = hasher.hashToString(12, password.toCharArray())

fun verifyPassword(password: String, encodedPassword: String): Boolean = verifier.verify(password.toCharArray(), encodedPassword).verified
