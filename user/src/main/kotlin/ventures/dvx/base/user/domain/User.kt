package ventures.dvx.base.user.domain

import arrow.core.ValidatedNel
import arrow.core.zip
import ventures.dvx.common.types.EmailAddress
import ventures.dvx.common.types.Msisdn
import ventures.dvx.common.types.NonEmptyString
import ventures.dvx.common.types.ValidationError

sealed class User {
  abstract val msisdn: Msisdn
  abstract val email: EmailAddress
  abstract val encryptedPassword: NonEmptyString
}

data class UnregisteredUser(
  override val msisdn: Msisdn,
  override val email : EmailAddress,
  override val encryptedPassword: NonEmptyString
) : User() {
  companion object {
    fun of(msisdn: String, email: String, encryptedPassword: String):
      ValidatedNel<ValidationError, UnregisteredUser> =
      validateAndCreate(msisdn, email, encryptedPassword) {
        m, e, p -> UnregisteredUser(m, e, p)
      }
  }
}

data class ActiveUser(
  override val msisdn: Msisdn,
  override val email: EmailAddress,
  override val encryptedPassword: NonEmptyString,
) : User() {
  companion object {
    fun of(msisdn: String, email: String, encryptedPassword: String):
      ValidatedNel<ValidationError, ActiveUser> =
      validateAndCreate(msisdn, email, encryptedPassword) {
        u, e, p -> ActiveUser(u, e, p)
      }
  }
}

data class AdminUser(
  override val msisdn: Msisdn,
  override val email: EmailAddress,
  override val encryptedPassword: NonEmptyString,
) : User() {
  companion object {
    fun of(msisdn: String, email: String, encryptedPassword: String):
      ValidatedNel<ValidationError, AdminUser> =
      validateAndCreate(msisdn, email, encryptedPassword) {
        u, e, p -> AdminUser(u, e, p)
      }
  }
}

data class DisabledUser(
  override val msisdn: Msisdn,
  override val email: EmailAddress,
  override val encryptedPassword: NonEmptyString,
) : User() {
  companion object {
    fun of(msisdn: String, email: String, encryptedPassword: String):
      ValidatedNel<ValidationError, DisabledUser> =
      validateAndCreate(msisdn, email, encryptedPassword) {
          u, e, p -> DisabledUser(u, e, p)
      }
  }
}

// DRYs up object creation
private fun <T> validateAndCreate(
  msisdn: String,
  email: String,
  encryptedPassword: String,
  createFn: (m: Msisdn, em: EmailAddress, pa: NonEmptyString) -> T) :
  ValidatedNel<ValidationError, T> =
  Msisdn.of(msisdn).zip(
    EmailAddress.of(email),
    NonEmptyString.of(encryptedPassword),
    createFn
  )
