package ventures.dvx.base.user.api

import java.util.*

// Commands

data class RegisterUserCommand(
  val msisdn: String, val email: String, val firstName: String, val lastName: String
)
data class LoginUserCommand(val msisdn: String)

data class ValidatePinCommand(val userId: UUID, val msisdn: String, val pin: String)

data class RegisterAdminCommand(val email: String, val password: String)

// Events

data class UserRegistrationStartedEvent(
  val userId: UUID,
  val email: String,
  val msisdn: String,
  val firstName: String,
  val lastName: String
)
data class UserRegisteredEvent(val userId: UUID)

data class AdminRegisteredEvent(val userId: UUID, val email: String, val encryptedPassword: String)

data class UserLoggedInEvent(val userId: UUID)

// Queries

data class UserQuery(val username: String)
