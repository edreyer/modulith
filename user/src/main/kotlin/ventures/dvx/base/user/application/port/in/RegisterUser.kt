package ventures.dvx.base.user.application.port.`in`

import arrow.core.Nel
import ventures.dvx.common.types.ValidationError
import ventures.dvx.common.workflow.Command
import ventures.dvx.common.workflow.Event
import ventures.dvx.common.workflow.SafeWorkflow

// Input

data class RegisterUserCommand(
  val username: String,
  val email: String,
  val password: String
) : Command

// Output

sealed class RegisterUserEvent : Event {
  data class ValidUserRegistration(
    val username: String,
    val email: String
  ) : RegisterUserEvent(), Event
}

// Errors

sealed class RegisterUserError : RuntimeException() {
  data class UserExistsError(val error: String) : RegisterUserError()
  data class UserValidationErrors(val errors: Nel<ValidationError>) : RegisterUserError()
}

// Workflow
interface RegisterUserWorkflow : SafeWorkflow<RegisterUserCommand, RegisterUserEvent>
