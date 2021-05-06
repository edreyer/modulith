package ventures.dvx.base.user.domain

import arrow.core.*
import ventures.dvx.common.types.EmailAddress
import ventures.dvx.common.types.NonEmptyString
import ventures.dvx.common.types.ValidationError

enum class Role {
  ROLE_USER,
  ROLE_ADMIN
}

sealed class User

data class UnregisteredUser(
  val username: String,
  val email : String,
  val encryptedPassword: String
) : User()

data class ActiveUser(
  val username: NonEmptyString,
  val email: EmailAddress,
  val encryptedPassword: NonEmptyString,
  val roles: NonEmptyList<Role>
) : User() {
  companion object {
    fun of(username: String, email: String, encryptedPassword: String, roles: NonEmptyList<Role> = Role.ROLE_USER.nel())
      : ValidatedNel<ValidationError, ActiveUser> =
      NonEmptyString.of(username).zip(
        EmailAddress.of(email),
        NonEmptyString.of(encryptedPassword)
      ) {u, e, p -> ActiveUser(u, e, p, roles)}
  }
}
