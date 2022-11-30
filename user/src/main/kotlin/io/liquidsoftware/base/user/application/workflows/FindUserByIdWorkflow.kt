package io.liquidsoftware.base.user.application.workflows

import arrow.core.continuations.EffectScope
import io.liquidsoftware.base.user.application.mapper.toUserDto
import io.liquidsoftware.base.user.application.port.`in`.FindUserByIdQuery
import io.liquidsoftware.base.user.application.port.`in`.UserFoundEvent
import io.liquidsoftware.base.user.application.port.`in`.UserNotFoundError
import io.liquidsoftware.base.user.application.port.out.FindUserPort
import io.liquidsoftware.common.workflow.BaseSafeWorkflow
import io.liquidsoftware.common.workflow.WorkflowDispatcher
import io.liquidsoftware.common.workflow.WorkflowError
import org.springframework.stereotype.Component
import javax.annotation.PostConstruct

@Component
internal class FindUserByIdWorkflow(
  private val findUserPort: FindUserPort
) : BaseSafeWorkflow<FindUserByIdQuery, UserFoundEvent>() {

  @PostConstruct
  fun registerWithDispatcher() = WorkflowDispatcher.registerQueryHandler(this)

  context(EffectScope<WorkflowError>)
  override suspend fun execute(request: FindUserByIdQuery): UserFoundEvent =
    findUserPort.findUserById(request.userId)
      ?.let { UserFoundEvent(it.toUserDto()) }
      ?: shift(UserNotFoundError(request.userId))
}
