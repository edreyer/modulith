package ventures.dvx.base.user.domain

import arrow.core.ValidatedNel
import arrow.core.zip
import ventures.dvx.common.types.EmailAddress
import ventures.dvx.common.types.NonEmptyString
import ventures.dvx.common.types.ValidationError

sealed class User {
  abstract val username: NonEmptyString
  abstract val email: EmailAddress
  abstract val encryptedPassword: NonEmptyString
}

data class UnregisteredUser(
  override val username: NonEmptyString,
  override val email : EmailAddress,
  override val encryptedPassword: NonEmptyString
) : User() {
  companion object {
    fun of(username: String, email: String, encryptedPassword: String):
      ValidatedNel<ValidationError, UnregisteredUser> =
      validateAndCreate(username, email, encryptedPassword) {
        u, e, p -> UnregisteredUser(u, e, p)
      }
  }
}

data class ActiveUser(
  override val username: NonEmptyString,
  override val email: EmailAddress,
  override val encryptedPassword: NonEmptyString,
) : User() {
  companion object {
    fun of(username: String, email: String, encryptedPassword: String):
      ValidatedNel<ValidationError, ActiveUser> =
      validateAndCreate(username, email, encryptedPassword) {
        u, e, p -> ActiveUser(u, e, p)
      }
  }
}

data class AdminUser(
  override val username: NonEmptyString,
  override val email: EmailAddress,
  override val encryptedPassword: NonEmptyString,
) : User() {
  companion object {
    fun of(username: String, email: String, encryptedPassword: String):
      ValidatedNel<ValidationError, AdminUser> =
      validateAndCreate(username, email, encryptedPassword) {
        u, e, p -> AdminUser(u, e, p)
      }
  }
}

// DRYs up object creation
private fun <T> validateAndCreate(
  username: String,
  email: String,
  encryptedPassword: String,
  createFn: (un: NonEmptyString, em: EmailAddress, pa: NonEmptyString) -> T) :
  ValidatedNel<ValidationError, T> =
  NonEmptyString.of(username).zip(
    EmailAddress.of(email),
    NonEmptyString.of(encryptedPassword),
    createFn
  )
