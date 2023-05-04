package io.liquidsoftware.base.user.application.workflows

import arrow.core.raise.Raise
import io.liquidsoftware.base.user.application.mapper.toUserDto
import io.liquidsoftware.base.user.application.port.`in`.FindUserByEmailQuery
import io.liquidsoftware.base.user.application.port.`in`.UserFoundEvent
import io.liquidsoftware.base.user.application.port.`in`.UserNotFoundError
import io.liquidsoftware.base.user.application.port.out.FindUserPort
import io.liquidsoftware.common.workflow.BaseSafeWorkflow
import io.liquidsoftware.common.workflow.WorkflowDispatcher
import io.liquidsoftware.common.workflow.WorkflowError
import jakarta.annotation.PostConstruct
import org.springframework.stereotype.Component

@Component
internal class FindUserByEmailWorkflow(
  private val findUserPort: FindUserPort
) : BaseSafeWorkflow<FindUserByEmailQuery, UserFoundEvent>() {

  @PostConstruct
  fun registerWithDispatcher() = WorkflowDispatcher.registerQueryHandler(this)

  context(Raise<WorkflowError>)
  override suspend fun execute(request: FindUserByEmailQuery): UserFoundEvent =
    findUserPort.findUserByEmail(request.email)
      ?.let { UserFoundEvent(it.toUserDto()) }
      ?: raise(UserNotFoundError(request.email))

}
