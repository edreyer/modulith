package io.liquidsoftware.base.user.application.workflows

import arrow.core.Either
import io.liquidsoftware.base.user.application.port.`in`.DisableUserCommand
import io.liquidsoftware.base.user.application.port.`in`.UserDisabledEvent
import io.liquidsoftware.base.user.application.port.`in`.UserDto
import io.liquidsoftware.base.user.application.port.out.FindUserPort
import io.liquidsoftware.base.user.application.port.out.UserEventPort
import io.liquidsoftware.common.workflow.WorkflowError as LegacyWorkflowError

internal class DisableUserUseCase(
  findUserPort: FindUserPort,
  userEventPort: UserEventPort,
) : UserAdminUseCase<UserDisabledEvent>(
  findUserPort = findUserPort,
  userEventPort = userEventPort,
  workflowId = "persist-user-disabled",
) {
  suspend fun execute(command: DisableUserCommand): Either<LegacyWorkflowError, UserDisabledEvent> =
    executeUserAdmin(command.userId)

  override fun toEvent(userDto: UserDto): UserDisabledEvent = UserDisabledEvent(userDto)
}
