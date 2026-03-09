package io.liquidsoftware.base.user.application.port.`in`

import arrow.core.Either
import io.liquidsoftware.common.usecase.AppEvent
import io.liquidsoftware.common.usecase.Command
import io.liquidsoftware.common.workflow.WorkflowError

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
data class UserExistsError(override val message: String) : WorkflowError(message)

interface RegisterUserApi {
  suspend fun registerUser(command: RegisterUserCommand): Either<WorkflowError, UserRegisteredEvent>
}
