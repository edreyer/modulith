package ventures.dvx.base.user.api

import org.axonframework.commandhandling.RoutingKey
import org.axonframework.modelling.command.TargetAggregateIdentifier
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
  @TargetAggregateIdentifier
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
  val userId: EndUserId,
  val email: String,
  val msisdn: String,
  val firstName: String,
  val lastName: String
) : IndexableAggregateEvent

data class EndUserLoginStartedEvent(
  val userId: EndUserId
)

data class AdminUserRegisteredEvent(
  override val ia: IndexableAggregateDto,
  val userId: AdminUserId,
  val password: String,
  val email: String,
  val firstName: String,
  val lastName: String
) : IndexableAggregateEvent

data class TokenValidatedEvent(
  val msisdnToken: MsisdnToken
)

// Queries

data class FindUserQuery(val username: String)
