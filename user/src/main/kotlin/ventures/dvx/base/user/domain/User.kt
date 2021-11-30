package ventures.dvx.base.user.domain

import arrow.core.Validated.Companion.validNel
import arrow.core.ValidatedNel
import arrow.core.zip
import org.valiktor.functions.matches
import org.valiktor.validate
import ventures.dvx.common.types.EmailAddress
import ventures.dvx.common.types.Msisdn
import ventures.dvx.common.types.NonEmptyString
import ventures.dvx.common.types.SimpleType
import ventures.dvx.common.types.ValidationError
import ventures.dvx.common.types.ensure

object UserNamespace {
  const val NAMESPACE = "u_"
}

class UserId private constructor(override val value: String)
  : SimpleType<String>() {
  companion object {
    fun of(value: String): ValidatedNel<ValidationError, UserId> = ensure {
      validate(UserId(value)) {
        validate(UserId::value).matches("${UserNamespace.NAMESPACE}.*".toRegex())
      }
    }
  }
}

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
      ValidatedNel<ValidationError, UnregisteredUser> =
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
      ValidatedNel<ValidationError, ActiveUser> =
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
      ValidatedNel<ValidationError, AdminUser> =
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
      ValidatedNel<ValidationError, DisabledUser> =
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
  ValidatedNel<ValidationError, T> =
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
  ValidatedNel<ValidationError, T> =
  UserId.of(id).zip(
    Msisdn.of(msisdn),
    EmailAddress.of(email),
    NonEmptyString.of(encryptedPassword),
    createFn
  )
