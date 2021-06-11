package ventures.dvx.base.user.api

import org.axonframework.modelling.command.TargetAggregateIdentifier
import ventures.dvx.common.axon.IndexableAggregateDto
import ventures.dvx.common.axon.IndexableAggregateEvent
import java.util.*

// Commands

data class RegisterUserCommand(
  val id: UUID,
  val msisdn: String,
  val email: String,
  val firstName: String,
  val lastName: String
)
data class LoginUserCommand(
  @TargetAggregateIdentifier val id: String,
  val msisdn: String
)
data class ValidatePinCommand(
  @TargetAggregateIdentifier val id: String,
  val userId: UUID,
  val msisdn: String,
  val pin: String
)
data class RegisterAdminCommand(
  @TargetAggregateIdentifier val id: String,
  val email: String,
  val password: String
)

// Events

data class UserRegistrationStartedEvent(
  override val ia: IndexableAggregateDto,
  val userId: UUID,
  val email: String,
  val msisdn: String,
  val firstName: String,
  val lastName: String
) : IndexableAggregateEvent
data class UserRegisteredEvent(val userId: UUID)
data class AdminRegisteredEvent(val userId: UUID, val email: String, val encryptedPassword: String)
data class UserLoggedInEvent(val userId: UUID)

// Queries

data class UserQuery(val username: String)
