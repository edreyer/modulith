package io.liquidsoftware.base.user.application.port.`in`

import arrow.core.Nel
import io.liquidsoftware.common.types.ValidationError
import io.liquidsoftware.common.workflow.Command
import io.liquidsoftware.common.workflow.Event

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
sealed class RegisterUserError : RuntimeException()

data class UserExistsError(val error: String) : RegisterUserError()
data class UserValidationErrors(val errors: Nel<ValidationError>) : RegisterUserError()

