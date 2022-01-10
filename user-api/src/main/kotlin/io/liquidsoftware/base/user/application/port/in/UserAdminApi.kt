package io.liquidsoftware.base.user.application.port.`in`

import io.liquidsoftware.common.workflow.Command
import io.liquidsoftware.common.workflow.Event

// Inputs

data class DisableUserCommand(val userId: String) : Command
data class EnableUserCommand(val userId: String) : Command

// Outputs

data class UserDisabledEvent(override val userDto: UserDto) : Event(), UserEvent
data class UserEnabledEvent(override val userDto: UserDto) : Event(), UserEvent
