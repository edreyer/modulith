package io.liquidsoftware.base.user.domain

import arrow.core.Nel
import arrow.core.continuations.EffectScope
import io.liquidsoftware.base.user.UserId
import io.liquidsoftware.common.security.acl.Acl
import io.liquidsoftware.common.security.acl.AclRole
import io.liquidsoftware.common.types.EmailAddress
import io.liquidsoftware.common.types.Msisdn
import io.liquidsoftware.common.types.NonEmptyString
import io.liquidsoftware.common.types.ValidationError
import io.liquidsoftware.common.types.ValidationErrors
import io.liquidsoftware.common.types.toEmailAddress
import io.liquidsoftware.common.types.toMsisdn
import io.liquidsoftware.common.types.toNonEmptyString

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

  companion object {
    context(EffectScope<ValidationErrors>)
    suspend fun newUserData(
      id: String,
      msisdn: String,
      email: String,
      encryptedPassword: String
    ) = UserData(
      UserId.of(id),
      msisdn.toMsisdn(),
      email.toEmailAddress(),
      encryptedPassword.toNonEmptyString()
    )
  }
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
    context(EffectScope<ValidationErrors>)
    suspend fun of(msisdn: String, email: String, encryptedPassword: String, role: Role): UnregisteredUser =
      UnregisteredUser(
        UserData(
          UserId.create(),
          msisdn.toMsisdn(),
          email.toEmailAddress(),
          encryptedPassword.toNonEmptyString()
        ),
        role)
  }
}

internal data class ActiveUser(
  private val data: UserData
) : User(), UserFields by data {
  companion object {
    context(EffectScope<ValidationErrors>)
    suspend fun of(id: String, msisdn: String, email: String, encryptedPassword: String): ActiveUser =
      ActiveUser(newUserData(id, msisdn, email, encryptedPassword))
  }
}

internal data class AdminUser(
  private val data: UserData
) : User(), UserFields by data {
  companion object {
    context(EffectScope<ValidationErrors>)
    suspend fun of(id: String, msisdn: String, email: String, encryptedPassword: String): AdminUser =
      AdminUser(newUserData(id, msisdn, email, encryptedPassword))
  }
}

  internal data class DisabledUser(
    private val data: UserData,
    val role: Role
  ) : User(), UserFields by data {
    companion object {
      context(EffectScope<ValidationErrors>)
      suspend fun of(id: String, msisdn: String, email: String, encryptedPassword: String, role: Role): DisabledUser =
        DisabledUser(newUserData(id, msisdn, email, encryptedPassword), role)
    }


  // DRYs up object creation
  context(EffectScope<ValidationErrors>)
  private suspend fun <T> validateAndCreate(
    id: String,
    msisdn: String,
    email: String,
    encryptedPassword: String,
    role: Role,
    createFn: (id: UserId, m: Msisdn, em: EmailAddress, pa: NonEmptyString, ro: Role) -> T
  ): T =
    createFn(
      UserId.of(id),
      Msisdn.of(msisdn),
      EmailAddress.of(email),
      NonEmptyString.of(encryptedPassword),
      role,
    )

  context(EffectScope<Nel<ValidationError>>)
  private suspend fun <T> validateAndCreate(
    id: String,
    msisdn: String,
    email: String,
    encryptedPassword: String,
    createFn: (id: UserId, m: Msisdn, em: EmailAddress, pa: NonEmptyString) -> T
  ): T =
    createFn(
      UserId.of(id),
      Msisdn.of(msisdn),
      EmailAddress.of(email),
      NonEmptyString.of(encryptedPassword)
    )

}
