package ventures.dvx.base.user.domain

import arrow.core.Nel
import arrow.core.NonEmptyList
import arrow.core.ValidatedNel
import arrow.core.nel
import arrow.core.zip
import ventures.dvx.common.types.EmailAddress
import ventures.dvx.common.types.NonEmptyString
import ventures.dvx.common.types.ValidationError

enum class Role {
  ROLE_USER,
  ROLE_ADMIN
}

sealed class User

data class UnregisteredUser(
  val username: NonEmptyString,
  val email : EmailAddress,
  val encryptedPassword: NonEmptyString
) : User() {
  companion object {
    fun of(username: String, email: String, encryptedPassword: String)
      : ValidatedNel<ValidationError, UnregisteredUser> =
      NonEmptyString.of(username).zip(
        EmailAddress.of(email),
        NonEmptyString.of(encryptedPassword)
      ) {u, e, p -> UnregisteredUser(u, e, p)}
  }
}

data class ActiveUser(
  val username: NonEmptyString,
  val email: EmailAddress,
  val encryptedPassword: NonEmptyString,
  val roles: NonEmptyList<Role>
) : User() {
  companion object {

    fun of(username: String, email: String, encryptedPassword: String, roles: Nel<Role> = Role.ROLE_USER.nel())
      : ValidatedNel<ValidationError, ActiveUser> =
      NonEmptyString.of(username).zip(
        EmailAddress.of(email),
        NonEmptyString.of(encryptedPassword)
      ) {u, e, p -> ActiveUser(u, e, p, roles)}

  }
}
