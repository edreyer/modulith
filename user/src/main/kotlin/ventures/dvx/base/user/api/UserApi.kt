package ventures.dvx.base.user.api

import org.axonframework.modelling.command.TargetAggregateIdentifier
import ventures.dvx.common.axon.IndexableAggregateDto
import ventures.dvx.common.axon.IndexableAggregateEvent
import java.time.Instant
import java.util.*

data class EndUserId(val id: UUID = UUID.randomUUID())
data class MsisdnTokenId(val id: UUID = UUID.randomUUID())

// Commands

data class RegisterUserCommand(
  val msisdn: String,
  val email: String,
  val firstName: String,
  val lastName: String
)
data class LoginUserCommand(
  @TargetAggregateIdentifier val id: String,
  val msisdn: String
)
data class CreateTokenCommand(
  val userId: EndUserId,
  val msisdn: String,
  val email: String
)
data class ValidatePinCommand(
  @TargetAggregateIdentifier val id: String,
  val userId: EndUserId,
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
data class TokenCreatedEvent(
  val id: MsisdnTokenId,
  val token: String,
  val msisdn: String,
  val email: String,
  val expires: Instant
  )
data class UserRegisteredEvent(val userId: UUID)
data class AdminRegisteredEvent(val userId: UUID, val email: String, val encryptedPassword: String)
data class UserLoggedInEvent(val userId: UUID)

// Queries

data class FindMsisdnToken(val msisdn: String, val token: String)
