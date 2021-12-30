package ventures.dvx.base.user.application.port.`in`

import arrow.core.Nel
import ventures.dvx.common.types.ValidationError
import ventures.dvx.common.workflow.Command
import ventures.dvx.common.workflow.Event

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
) : Event(), UserEvent

// Errors
sealed class RegisterUserError : RuntimeException() {
  data class UserExistsError(val error: String) : RegisterUserError()
  data class UserValidationErrors(val errors: Nel<ValidationError>) : RegisterUserError()
}
