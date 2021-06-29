package ventures.dvx.base.user.api

import arrow.core.ValidatedNel
import arrow.core.validNel
import arrow.core.zip
import org.axonframework.commandhandling.RoutingKey
import org.axonframework.modelling.command.TargetAggregateIdentifier
import org.springframework.http.HttpStatus
import ventures.dvx.base.user.command.MsisdnToken
import ventures.dvx.base.user.command.SecuredCommand
import ventures.dvx.bridgekeeper.Operation
import ventures.dvx.common.axon.IndexableAggregateDto
import ventures.dvx.common.axon.IndexableAggregateEvent
import ventures.dvx.common.types.EmailAddress
import ventures.dvx.common.types.Msisdn
import ventures.dvx.common.types.NonEmptyString
import ventures.dvx.common.types.ValidationError
import java.util.*

// public API

data class EndUserId(val id: UUID = UUID.randomUUID())
data class AdminUserId(val id: UUID = UUID.randomUUID())
data class User(val id: UUID, val username: String, val password: String, val email: String, val roles: List<String>)

// Commands

data class RegisterEndUserCommand(
  @RoutingKey
  val userId: EndUserId,
  val msisdn: Msisdn,
  val email: EmailAddress,
  val firstName: NonEmptyString,
  val lastName: NonEmptyString
) {
  companion object {
    fun of(userId: EndUserId, msisdn: String, email: String, firstName: String, lastName: String):
      ValidatedNel<ValidationError, RegisterEndUserCommand> =
      userId.validNel().zip(
        Msisdn.of(msisdn),
        EmailAddress.of(email),
        NonEmptyString.of(firstName),
        NonEmptyString.of(lastName)
      ) { id, m, e, f, l ->
        RegisterEndUserCommand(id, m, e, f, l)
      }
  }
}

data class RegisterAdminUserCommand(
  @RoutingKey
  val userId: AdminUserId,
  val email: EmailAddress,
  val plainPassword: NonEmptyString, // unencrypted password
  val firstName: NonEmptyString,
  val lastName: NonEmptyString
) {
  companion object {
    fun of(userId: AdminUserId, email: String, plainPassword: String, firstName: String, lastName: String):
      ValidatedNel<ValidationError, RegisterAdminUserCommand> =
      userId.validNel().zip(
        EmailAddress.of(email),
        NonEmptyString.of(plainPassword),
        NonEmptyString.of(firstName),
        NonEmptyString.of(lastName)
      ) { id, e, p, f, l ->
        RegisterAdminUserCommand(id, e, p, f, l)
      }
  }
}

data class LoginEndUserCommand(
  @TargetAggregateIdentifier
  val userId: EndUserId,
  val msisdn: Msisdn
){
  companion object {
    fun of(userId: EndUserId, msisdn: String):
      ValidatedNel<ValidationError, LoginEndUserCommand> =
      userId.validNel().zip(Msisdn.of(msisdn)) {
          id, m -> LoginEndUserCommand(id, m)
      }
  }
}


data class ValidateEndUserTokenCommand(
  @TargetAggregateIdentifier
  val userId: EndUserId,
  val msisdn: Msisdn,
  val token: NonEmptyString
) {
  companion object {
    fun of(userId: EndUserId, msisdn: String, token: String):
      ValidatedNel<ValidationError, ValidateEndUserTokenCommand> =
      userId.validNel().zip(
        Msisdn.of(msisdn),
        NonEmptyString.of(token)
      ) { id, e, t ->
        ValidateEndUserTokenCommand(id, e, t)
      }
  }
}


// Events

data class UserRegistrationStartedEvent(
  override val ia: IndexableAggregateDto,
  val token: MsisdnToken,
  val userId: EndUserId,
  val firstName: NonEmptyString,
  val lastName: NonEmptyString
) : IndexableAggregateEvent

data class EndUserLoginStartedEvent(val token: MsisdnToken)

data class AdminUserRegisteredEvent(
  override val ia: IndexableAggregateDto,
  val userId: AdminUserId,
  val password: NonEmptyString,
  val email: EmailAddress,
  val firstName: NonEmptyString,
  val lastName: NonEmptyString
) : IndexableAggregateEvent

class TokenValidatedEvent

// Queries

data class FindUserByIdQuery(val id: UUID)
data class FindUserByUsernameQuery(val username: String)

// Errors

sealed class UserError {
  abstract val responseStatus: HttpStatus
  abstract val msg: String
}
data class InvalidInput(val errors: List<String>): UserError() {
  override val responseStatus = HttpStatus.BAD_REQUEST
  override val msg = "Validation errors: \n${errors.joinToString { "$it,\n" }}"
}
data class EndUserExistsError(val msisdn: Msisdn): UserError() {
  override val responseStatus = HttpStatus.BAD_REQUEST
  override val msg = "User already exists with phone: '${msisdn.value}'"
}
data class AdminUserExistsError(val email: EmailAddress): UserError() {
  override val responseStatus = HttpStatus.BAD_REQUEST
  override val msg = "Admin already exists with email: '${email.value}'"
}
data class UserNotFoundError(val username: String): UserError() {
  override val responseStatus = HttpStatus.UNAUTHORIZED
  override val msg = "User not found with username: '$username'"
}
data class UnauthorizedCommand(
  val user: String,
  val secureCommand: SecuredCommand,
  val aggregateId: String? = "[new object]")
  : UserError() {
  override val responseStatus = HttpStatus.UNAUTHORIZED
  val command = Operation(secureCommand.javaClass.simpleName)
  override val msg = "User $user is not authorized to perform $command on aggregate $aggregateId"
}
object InvalidTokenError: UserError() {
  override val responseStatus = HttpStatus.UNAUTHORIZED
  override val msg = "Invalid username or password"
}
