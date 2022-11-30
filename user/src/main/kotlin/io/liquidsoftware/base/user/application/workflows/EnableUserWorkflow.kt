package io.liquidsoftware.base.user.application.workflows

import arrow.core.continuations.EffectScope
import io.liquidsoftware.base.user.application.mapper.toUserDto
import io.liquidsoftware.base.user.application.port.`in`.EnableUserCommand
import io.liquidsoftware.base.user.application.port.`in`.UserEnabledEvent
import io.liquidsoftware.base.user.application.port.`in`.UserNotFoundError
import io.liquidsoftware.base.user.application.port.out.FindUserPort
import io.liquidsoftware.base.user.application.port.out.UserEventPort
import io.liquidsoftware.common.workflow.BaseSafeWorkflow
import io.liquidsoftware.common.workflow.WorkflowDispatcher
import io.liquidsoftware.common.workflow.WorkflowError
import org.springframework.stereotype.Component
import javax.annotation.PostConstruct

@Component
internal class EnableUserWorkflow(
  private val findUserPort: FindUserPort,
  private val userEventPort: UserEventPort
) : BaseSafeWorkflow<EnableUserCommand, UserEnabledEvent>() {

  @PostConstruct
  fun registerWithDispatcher() = WorkflowDispatcher.registerCommandHandler(this)

  context(EffectScope<WorkflowError>)
  override suspend fun execute(request: EnableUserCommand): UserEnabledEvent =
    findUserPort.findUserById(request.userId)
      ?.let {userEventPort.handle(UserEnabledEvent(it.toUserDto())) }
      ?: shift(UserNotFoundError("User not found with ID ${request.userId}"))

}
