package io.liquidsoftware.base.user.application.workflows

import arrow.core.raise.Raise
import io.liquidsoftware.base.user.application.port.`in`.FindUserByMsisdnQuery
import io.liquidsoftware.base.user.application.port.`in`.UserFoundEvent
import io.liquidsoftware.base.user.application.port.out.FindUserPort
import arrow.core.raise.context.bind
import io.liquidsoftware.common.workflow.BaseSafeWorkflow
import io.liquidsoftware.common.workflow.WorkflowError
import io.liquidsoftware.common.workflow.WorkflowRegistry
import org.springframework.stereotype.Component

@Component
internal class FindUserByMsisdnWorkflow(
  findUserPort: FindUserPort,
) : BaseSafeWorkflow<FindUserByMsisdnQuery, UserFoundEvent>() {

  private val useCase = FindUserByMsisdnUseCase(findUserPort)

  override fun registerWithDispatcher() = WorkflowRegistry.registerQueryHandler(this)

  context(_: Raise<WorkflowError>)
  override suspend fun execute(request: FindUserByMsisdnQuery): UserFoundEvent =
    useCase.execute(request).bind()
}
