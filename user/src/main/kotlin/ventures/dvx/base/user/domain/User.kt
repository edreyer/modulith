package ventures.dvx.base.user.domain

import arrow.core.Validated.Companion.validNel
import arrow.core.zip
import ventures.dvx.base.user.UserId
import ventures.dvx.common.types.EmailAddress
import ventures.dvx.common.types.Msisdn
import ventures.dvx.common.types.NonEmptyString
import ventures.dvx.common.types.ValidationErrorNel

/**
 * Delegate UserData class to cut down on copy-pasta in each ADT instance of User
 * see: https://proandroiddev.com/simpler-kotlin-class-hierarchies-using-class-delegation-35464106fed5
 */
internal interface UserFields {
  val msisdn: Msisdn
  val email: EmailAddress
  val encryptedPassword: NonEmptyString
}

internal data class UserData(
  override val msisdn: Msisdn,
  override val email: EmailAddress,
  override val encryptedPassword: NonEmptyString
) : UserFields

internal sealed class User: UserFields {
  abstract val id: UserId
}

internal enum class Role {
  ROLE_USER,
  ROLE_ADMIN
}

internal data class UnregisteredUser(
  private val data: UserData,
  val role: Role
) : UserFields by data {
  companion object {
    fun of(msisdn: String, email: String, encryptedPassword: String, role: Role):
      ValidationErrorNel<UnregisteredUser> =
      Msisdn.of(msisdn).zip(
        EmailAddress.of(email),
        NonEmptyString.of(encryptedPassword),
        validNel(role),
      ) { m, e, p, r -> UnregisteredUser(UserData(m, e, p), r) }
  }
}

internal data class ActiveUser(
  override val id: UserId,
  private val data: UserData
) : User(), UserFields by data {
  companion object {
    fun of(id: String, msisdn: String, email: String, encryptedPassword: String):
      ValidationErrorNel<ActiveUser> =
      validateAndCreate(id, msisdn, email, encryptedPassword) {
        i, u, e, p -> ActiveUser(i, UserData(u, e, p))
      }
  }
}

internal data class AdminUser(
  override val id: UserId,
  private val data: UserData
) : User(), UserFields by data {
  companion object {
    fun of(id: String, msisdn: String, email: String, encryptedPassword: String):
      ValidationErrorNel<AdminUser> =
      validateAndCreate(id, msisdn, email, encryptedPassword) {
          i, u, e, p -> AdminUser(i, UserData(u, e, p))
      }
  }
}

internal data class DisabledUser(
  override val id: UserId,
  private val data: UserData,
  val role: Role
  ) : User(), UserFields by data {
  companion object {
    fun of(id: String, msisdn: String, email: String, encryptedPassword: String, role: Role):
      ValidationErrorNel<DisabledUser> =
      validateAndCreate(id, msisdn, email, encryptedPassword, role) {
          i, u, e, p, r -> DisabledUser(i, UserData(u, e, p), r)
      }
  }

}

// DRYs up object creation
private fun <T> validateAndCreate(
  id: String,
  msisdn: String,
  email: String,
  encryptedPassword: String,
  role: Role,
  createFn: (id: UserId, m: Msisdn, em: EmailAddress, pa: NonEmptyString, ro: Role) -> T) :
  ValidationErrorNel<T> =
  UserId.of(id).zip(
    Msisdn.of(msisdn),
    EmailAddress.of(email),
    NonEmptyString.of(encryptedPassword),
    validNel(role),
    createFn
  )

private fun <T> validateAndCreate(
  id: String,
  msisdn: String,
  email: String,
  encryptedPassword: String,
  createFn: (id: UserId, m: Msisdn, em: EmailAddress, pa: NonEmptyString) -> T) :
  ValidationErrorNel<T> =
  UserId.of(id).zip(
    Msisdn.of(msisdn),
    EmailAddress.of(email),
    NonEmptyString.of(encryptedPassword),
    createFn
  )
