package ventures.dvx.base.user.application.port.`in`

import ventures.dvx.common.workflow.BaseSafeSecureWorkflow
import ventures.dvx.common.workflow.Command
import ventures.dvx.common.workflow.Event

// Inputs

data class DisableUserCommand(val userId: String) : Command
data class EnableUserCommand(val userId: String) : Command

// Outputs

data class UserDisabledEvent(override val userDto: UserDto) : Event(), UserEvent
data class UserEnabledEvent(override val userDto: UserDto) : Event(), UserEvent

// Workflows

abstract class DisableUserWorkflow :
  BaseSafeSecureWorkflow<DisableUserCommand, UserDisabledEvent>(),
  UserSecured<DisableUserCommand>

abstract class EnableUserWorkflow :
  BaseSafeSecureWorkflow<EnableUserCommand, UserEnabledEvent>(),
  UserSecured<EnableUserCommand>
