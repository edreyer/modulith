package io.liquidsoftware.base.user.application.workflows

import arrow.core.Either
import io.liquidsoftware.base.user.application.port.`in`.EnableUserCommand
import io.liquidsoftware.base.user.application.port.`in`.UserDto
import io.liquidsoftware.base.user.application.port.`in`.UserEnabledEvent
import io.liquidsoftware.base.user.application.port.out.FindUserPort
import io.liquidsoftware.base.user.application.port.out.UserEventPort
import io.liquidsoftware.common.application.error.ApplicationError

internal class EnableUserUseCase(
  findUserPort: FindUserPort,
  userEventPort: UserEventPort,
) : UserAdminUseCase<UserEnabledEvent>(
  findUserPort = findUserPort,
  userEventPort = userEventPort,
  workflowId = "persist-user-enabled",
) {
  suspend fun execute(command: EnableUserCommand): Either<ApplicationError, UserEnabledEvent> =
    executeUserAdmin(command.userId)

  override fun toEvent(userDto: UserDto): UserEnabledEvent = UserEnabledEvent(userDto)
}
