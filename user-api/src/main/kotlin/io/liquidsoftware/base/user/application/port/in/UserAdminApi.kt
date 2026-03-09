package io.liquidsoftware.base.user.application.port.`in`

import arrow.core.Either
import io.liquidsoftware.common.usecase.AppEvent
import io.liquidsoftware.common.usecase.Command
import io.liquidsoftware.common.workflow.WorkflowError

// Inputs
data class DisableUserCommand(val userId: String) : Command
data class EnableUserCommand(val userId: String) : Command

// Outputs
data class UserDisabledEvent(override val userDto: UserDto) : AppEvent(), UserEvent
data class UserEnabledEvent(override val userDto: UserDto) : AppEvent(), UserEvent

interface UserAdminApi {
  suspend fun enableUser(command: EnableUserCommand): Either<WorkflowError, UserEnabledEvent>
  suspend fun disableUser(command: DisableUserCommand): Either<WorkflowError, UserDisabledEvent>
}
