package io.liquidsoftware.base.user.application.workflows

import arrow.core.raise.Raise
import io.liquidsoftware.base.user.application.port.`in`.FindUserByEmailQuery
import io.liquidsoftware.base.user.application.port.`in`.UserFoundEvent
import io.liquidsoftware.base.user.application.port.out.FindUserPort
import arrow.core.raise.context.bind
import io.liquidsoftware.common.workflow.BaseSafeWorkflow
import io.liquidsoftware.common.workflow.WorkflowError
import io.liquidsoftware.common.workflow.WorkflowRegistry
import org.springframework.stereotype.Component

@Component
internal class FindUserByEmailWorkflow(
  findUserPort: FindUserPort,
) : BaseSafeWorkflow<FindUserByEmailQuery, UserFoundEvent>() {

  private val useCase = FindUserByEmailUseCase(findUserPort)

  override fun registerWithDispatcher() = WorkflowRegistry.registerQueryHandler(this)

  context(_: Raise<WorkflowError>)
  override suspend fun execute(request: FindUserByEmailQuery): UserFoundEvent =
    useCase.execute(request).bind()
}
