package ventures.dvx.base.user.api

import org.axonframework.modelling.command.TargetAggregateIdentifier
import ventures.dvx.base.user.command.EndUserId
import ventures.dvx.base.user.command.MsisdnToken
import ventures.dvx.common.axon.IndexableAggregateDto
import ventures.dvx.common.axon.IndexableAggregateEvent
import java.util.*

// public API

data class EndUserId(val id: UUID = UUID.randomUUID())
data class User(val id: UUID, val email: String, val username: String, val roles: List<String>)

// Commands

data class RegisterUserCommand(
  @TargetAggregateIdentifier
  val userId: EndUserId,
  val msisdn: String,
  val email: String,
  val firstName: String,
  val lastName: String
)
data class LoginUserCommand(
  @TargetAggregateIdentifier
  val id: String,
  val msisdn: String
)
data class ValidateEndUserTokenCommand(
  @TargetAggregateIdentifier val id: EndUserId,
  val msisdn: String,
  val token: String
)

data class RegisterAdminCommand(
  @TargetAggregateIdentifier val id: String,
  val email: String,
  val password: String
)

// Events

data class UserRegistrationStartedEvent(
  override val ia: IndexableAggregateDto,
  val userId: EndUserId,
  val email: String,
  val msisdn: String,
  val firstName: String,
  val lastName: String
) : IndexableAggregateEvent
data class ValidTokenEvent(
  val msisdnToken: MsisdnToken
)
data class UserRegisteredEvent(val userId: UUID)
data class AdminRegisteredEvent(val userId: UUID, val email: String, val encryptedPassword: String)
data class UserLoggedInEvent(val userId: UUID)

// Queries

data class FindUserQuery(val username: String)
