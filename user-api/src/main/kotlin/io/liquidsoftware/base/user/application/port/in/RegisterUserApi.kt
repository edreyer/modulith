package io.liquidsoftware.base.user.application.port.`in`

import arrow.core.Either
import io.liquidsoftware.common.application.error.ApplicationError
import io.liquidsoftware.common.application.error.ConflictApplicationError
import io.liquidsoftware.common.usecase.AppEvent
import io.liquidsoftware.common.usecase.Command

// Input
data class RegisterUserCommand(
  val msisdn: String,
  val email: String,
  val password: String,
  val role: String
) : Command

// Event
data class UserRegisteredEvent(
  override val userDto: UserDto,
  val password: String
) : AppEvent(), UserEvent

// Errors
data class UserExistsError(
  override val message: String,
) : ConflictApplicationError {
  override val code: String = "USER_EXISTS"
  override val metadata: Map<String, String> = emptyMap()
}

interface RegisterUserApi {
  suspend fun registerUser(command: RegisterUserCommand): Either<ApplicationError, UserRegisteredEvent>
}
