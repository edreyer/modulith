package ventures.dvx.base.user.domain

import arrow.core.ValidatedNel
import arrow.core.zip
import ventures.dvx.common.types.EmailAddress
import ventures.dvx.common.types.Msisdn
import ventures.dvx.common.types.NonEmptyString
import ventures.dvx.common.types.ValidationError

interface UserFields {
  val msisdn: Msisdn
  val email: EmailAddress
  val encryptedPassword: NonEmptyString
}

/**
 * Delegate class to cut down on copy-pasta in each ADT instance of User
 */
data class UserData(
  override val msisdn: Msisdn,
  override val email: EmailAddress,
  override val encryptedPassword: NonEmptyString
) : UserFields

sealed class User: UserFields

data class UnregisteredUser(
  val data: UserData,
) : User(), UserFields by data {
  companion object {
    fun of(msisdn: String, email: String, encryptedPassword: String):
      ValidatedNel<ValidationError, UnregisteredUser> =
      validateAndCreate(msisdn, email, encryptedPassword) {
        m, e, p -> UnregisteredUser(UserData(m, e, p))
      }
  }
}

data class ActiveUser(
  private val data: UserData
) : User(), UserFields by data {
  companion object {
    fun of(msisdn: String, email: String, encryptedPassword: String):
      ValidatedNel<ValidationError, ActiveUser> =
      validateAndCreate(msisdn, email, encryptedPassword) {
        u, e, p -> ActiveUser(UserData(u, e, p))
      }
  }
}

data class AdminUser(
  private val data: UserData
) : User(), UserFields by data {
  companion object {
    fun of(msisdn: String, email: String, encryptedPassword: String):
      ValidatedNel<ValidationError, AdminUser> =
      validateAndCreate(msisdn, email, encryptedPassword) {
        u, e, p -> AdminUser(UserData(u, e, p))
      }
  }
}

data class DisabledUser(
  private val data: UserData
) : User(), UserFields by data {
  companion object {
    fun of(msisdn: String, email: String, encryptedPassword: String):
      ValidatedNel<ValidationError, DisabledUser> =
      validateAndCreate(msisdn, email, encryptedPassword) {
          u, e, p -> DisabledUser(UserData(u, e, p))
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
