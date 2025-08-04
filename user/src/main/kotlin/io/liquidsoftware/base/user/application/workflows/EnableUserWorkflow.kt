package io.liquidsoftware.base.user.application.workflows

import arrow.core.raise.Raise
import io.liquidsoftware.base.user.application.mapper.toUserDto
import io.liquidsoftware.base.user.application.port.`in`.EnableUserCommand
import io.liquidsoftware.base.user.application.port.`in`.UserEnabledEvent
import io.liquidsoftware.base.user.application.port.`in`.UserNotFoundError
import io.liquidsoftware.base.user.application.port.out.FindUserPort
import io.liquidsoftware.base.user.application.port.out.UserEventPort
import io.liquidsoftware.common.ext.raise
import io.liquidsoftware.common.workflow.BaseSafeWorkflow
import io.liquidsoftware.common.workflow.WorkflowError
import io.liquidsoftware.common.workflow.WorkflowRegistry
import org.springframework.stereotype.Component

@Component
internal class EnableUserWorkflow(
  private val findUserPort: FindUserPort,
  private val userEventPort: UserEventPort
) : BaseSafeWorkflow<EnableUserCommand, UserEnabledEvent>() {

  override fun registerWithDispatcher() = WorkflowRegistry.registerCommandHandler(this)

  context(_: Raise<WorkflowError>)
  override suspend fun execute(request: EnableUserCommand): UserEnabledEvent =
    findUserPort.findUserById(request.userId)
      ?.let {userEventPort.handle(UserEnabledEvent(it.toUserDto())) }
      ?: raise(UserNotFoundError("User not found with ID ${request.userId}"))

}
