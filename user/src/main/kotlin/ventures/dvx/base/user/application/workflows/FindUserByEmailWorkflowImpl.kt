package ventures.dvx.base.user.application.workflows

import arrow.core.computations.result
import org.springframework.stereotype.Component
import ventures.dvx.base.user.application.port.`in`.FindUserByEmailQuery
import ventures.dvx.base.user.application.port.`in`.FindUserByEmailWorkflow
import ventures.dvx.base.user.application.port.`in`.FindUserEvent
import ventures.dvx.base.user.application.port.`in`.UserNotFoundError
import ventures.dvx.base.user.application.port.out.FindUserPort
import ventures.dvx.base.user.application.workflows.mapper.toUserDto
import ventures.dvx.common.workflow.WorkflowDispatcher
import javax.annotation.PostConstruct

@Component
class FindUserByEmailWorkflowImpl(
  private val findUserPort: FindUserPort
) : FindUserByEmailWorkflow {

  @PostConstruct
  fun registerWithDispatcher() {
    WorkflowDispatcher.registerQueryHandler(this)
  }

  override suspend operator fun invoke(request: FindUserByEmailQuery): Result<FindUserEvent> =
    result {
      findUserPort.findUserByEmail(request.email)
        ?.let { FindUserEvent(it.toUserDto()) }
        ?: throw UserNotFoundError(request.email)
    }

}
