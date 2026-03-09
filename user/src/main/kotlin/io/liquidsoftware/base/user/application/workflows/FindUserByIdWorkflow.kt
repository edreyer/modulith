package io.liquidsoftware.base.user.application.workflows

import arrow.core.raise.Raise
import io.liquidsoftware.base.user.application.port.`in`.FindUserByIdQuery
import io.liquidsoftware.base.user.application.port.`in`.UserFoundEvent
import io.liquidsoftware.base.user.application.port.out.FindUserPort
import arrow.core.raise.context.bind
import io.liquidsoftware.common.workflow.BaseSafeWorkflow
import io.liquidsoftware.common.workflow.WorkflowError
import io.liquidsoftware.common.workflow.WorkflowRegistry
import org.springframework.stereotype.Component

@Component
internal class FindUserByIdWorkflow(
  findUserPort: FindUserPort,
) : BaseSafeWorkflow<FindUserByIdQuery, UserFoundEvent>() {

  private val useCase = FindUserByIdUseCase(findUserPort)

  override fun registerWithDispatcher() = WorkflowRegistry.registerQueryHandler(this)

  context(_: Raise<WorkflowError>)
  override suspend fun execute(request: FindUserByIdQuery): UserFoundEvent =
    useCase.execute(request).bind()
}
