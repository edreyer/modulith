package io.liquidsoftware.base.user.domain

import arrow.core.Validated.Companion.validNel
import arrow.core.zip
import io.liquidsoftware.base.user.UserId
import io.liquidsoftware.common.security.acl.Acl
import io.liquidsoftware.common.security.acl.AclRole
import io.liquidsoftware.common.types.EmailAddress
import io.liquidsoftware.common.types.Msisdn
import io.liquidsoftware.common.types.NonEmptyString
import io.liquidsoftware.common.types.ValidationErrorNel

/**
 * Delegate UserData class to cut down on copy-pasta in each ADT instance of User
 * see: https://proandroiddev.com/simpler-kotlin-class-hierarchies-using-class-delegation-35464106fed5
 */
internal interface UserFields {
  val id: UserId
  val msisdn: Msisdn
  val email: EmailAddress
  val encryptedPassword: NonEmptyString
}

internal data class UserData(
  override val id: UserId,
  override val msisdn: Msisdn,
  override val email: EmailAddress,
  override val encryptedPassword: NonEmptyString
) : UserFields

internal sealed class User: UserFields {
  fun acl() = Acl.of(id.value, id.value, AclRole.MANAGER)
}

internal enum class Role {
  ROLE_USER,
  ROLE_ADMIN
}

internal data class UnregisteredUser(
  private val data: UserData,
  val role: Role
) : User(), UserFields by data {
  companion object {
    fun of(msisdn: String, email: String, encryptedPassword: String, role: Role):
      ValidationErrorNel<UnregisteredUser> =
      UserId.create().zip(
        Msisdn.of(msisdn),
        EmailAddress.of(email),
        NonEmptyString.of(encryptedPassword),
        validNel(role),
      ) { i, m, e, p, r -> UnregisteredUser(UserData(i, m, e, p), r) }
  }
}

internal data class ActiveUser(
  private val data: UserData
) : User(), UserFields by data {
  companion object {
    fun of(id: String, msisdn: String, email: String, encryptedPassword: String):
      ValidationErrorNel<ActiveUser> =
      validateAndCreate(id, msisdn, email, encryptedPassword) {
        i, u, e, p -> ActiveUser(UserData(i, u, e, p))
      }
  }
}

internal data class AdminUser(
  private val data: UserData
) : User(), UserFields by data {
  companion object {
    fun of(id: String, msisdn: String, email: String, encryptedPassword: String):
      ValidationErrorNel<AdminUser> =
      validateAndCreate(id, msisdn, email, encryptedPassword) {
          i, u, e, p -> AdminUser(UserData(i, u, e, p))
      }
  }
}

internal data class DisabledUser(
  private val data: UserData,
  val role: Role
  ) : User(), UserFields by data {
  companion object {
    fun of(id: String, msisdn: String, email: String, encryptedPassword: String, role: Role):
      ValidationErrorNel<DisabledUser> =
      validateAndCreate(id, msisdn, email, encryptedPassword, role) {
          i, u, e, p, r -> DisabledUser(UserData(i, u, e, p), r)
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
