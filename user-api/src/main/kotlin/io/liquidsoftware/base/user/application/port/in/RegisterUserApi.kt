package io.liquidsoftware.base.user.application.port.`in`

import io.liquidsoftware.common.workflow.Command
import io.liquidsoftware.common.workflow.Event
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
) : Event(), UserEvent

// Errors
data class UserExistsError(override val message: String) : WorkflowError(message)


