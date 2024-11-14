package io.liquidsoftware.base.user.application.workflows

import arrow.core.raise.Raise
import io.liquidsoftware.base.user.application.mapper.toUserDto
import io.liquidsoftware.base.user.application.port.`in`.DisableUserCommand
import io.liquidsoftware.base.user.application.port.`in`.UserDisabledEvent
import io.liquidsoftware.base.user.application.port.`in`.UserNotFoundError
import io.liquidsoftware.base.user.application.port.out.FindUserPort
import io.liquidsoftware.base.user.application.port.out.UserEventPort
import io.liquidsoftware.common.workflow.BaseSafeWorkflow
import io.liquidsoftware.common.workflow.WorkflowDispatcher
import io.liquidsoftware.common.workflow.WorkflowError
import jakarta.annotation.PostConstruct
import org.springframework.stereotype.Component

@Component
internal class DisableUserWorkflow(
  private val findUserPort: FindUserPort,
  private val userEventPort: UserEventPort
) : BaseSafeWorkflow<DisableUserCommand, UserDisabledEvent>() {

  override fun registerWithDispatcher() = WorkflowDispatcher.registerCommandHandler(this)

  context(Raise<WorkflowError>)
  override suspend fun execute(request: DisableUserCommand): UserDisabledEvent =
    findUserPort.findUserById(request.userId)
      ?.let { userEventPort.handle(UserDisabledEvent(it.toUserDto())) }
      ?: raise(UserNotFoundError("User not found with ID ${request.userId}"))

}
