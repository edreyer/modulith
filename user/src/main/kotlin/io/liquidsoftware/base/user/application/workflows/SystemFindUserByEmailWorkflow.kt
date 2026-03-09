package io.liquidsoftware.base.user.application.workflows

import arrow.core.raise.Raise
import io.liquidsoftware.base.user.application.port.`in`.SystemFindUserByEmailQuery
import io.liquidsoftware.base.user.application.port.`in`.SystemUserFoundEvent
import arrow.core.raise.context.bind
import io.liquidsoftware.common.workflow.BaseSafeWorkflow
import io.liquidsoftware.common.workflow.WorkflowError
import io.liquidsoftware.common.workflow.WorkflowRegistry
import org.springframework.stereotype.Component

@Component
internal class SystemFindUserByEmailWorkflow(
  private val systemFindUserByEmailUseCase: SystemFindUserByEmailUseCase,
) : BaseSafeWorkflow<SystemFindUserByEmailQuery, SystemUserFoundEvent>() {

  override fun registerWithDispatcher() = WorkflowRegistry.registerQueryHandler(this)

  context(_: Raise<WorkflowError>)
  override suspend fun execute(request: SystemFindUserByEmailQuery): SystemUserFoundEvent =
    systemFindUserByEmailUseCase.execute(request).bind()
}
