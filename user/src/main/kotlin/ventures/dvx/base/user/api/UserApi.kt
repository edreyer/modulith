package ventures.dvx.base.user.api

import org.axonframework.commandhandling.RoutingKey
import org.axonframework.modelling.command.TargetAggregateIdentifier
import org.springframework.http.HttpStatus
import ventures.dvx.base.user.command.MsisdnToken
import ventures.dvx.common.axon.IndexableAggregateDto
import ventures.dvx.common.axon.IndexableAggregateEvent
import java.util.*

// public API

data class EndUserId(val id: UUID = UUID.randomUUID())
data class AdminUserId(val id: UUID = UUID.randomUUID())
data class User(val id: UUID, val username: String, val password: String, val email: String, val roles: List<String>)

// Commands

data class RegisterEndUserCommand(
  @RoutingKey
  val userId: EndUserId,
  val msisdn: String,
  val email: String,
  val firstName: String,
  val lastName: String
)

data class RegisterAdminUserCommand(
  @RoutingKey
  val userId: AdminUserId,
  val plainPassword: String, // unencrypted password
  val email: String,
  val firstName: String,
  val lastName: String
)

data class LoginEndUserCommand(
  @TargetAggregateIdentifier
  val userId: EndUserId,
  val msisdn: String
)

data class ValidateEndUserTokenCommand(
  @TargetAggregateIdentifier
  val userId: EndUserId,
  val msisdn: String,
  val token: String
)

// Events

data class UserRegistrationStartedEvent(
  override val ia: IndexableAggregateDto,
  val token: MsisdnToken,
  val userId: EndUserId,
  val firstName: String,
  val lastName: String
) : IndexableAggregateEvent

data class EndUserLoginStartedEvent(val token: MsisdnToken)

data class AdminUserRegisteredEvent(
  override val ia: IndexableAggregateDto,
  val userId: AdminUserId,
  val password: String,
  val email: String,
  val firstName: String,
  val lastName: String
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
data class EndUserExistsError(val msisdn: String): UserError() {
  override val responseStatus = HttpStatus.BAD_REQUEST
  override val msg = "User already exists with phone: '$msisdn'"
}
data class AdminUserExistsError(val email: String): UserError() {
  override val responseStatus = HttpStatus.BAD_REQUEST
  override val msg = "Admin already exists with email: '$email'"
}
data class UserNotFoundError(val username: String): UserError() {
  override val responseStatus = HttpStatus.UNAUTHORIZED
  override val msg = "User not found with username: '$username'"
}
object InvalidTokenError: UserError() {
  override val responseStatus = HttpStatus.UNAUTHORIZED
  override val msg = "Invalid username or password"
}
