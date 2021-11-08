package ventures.dvx.base.user.application.workflows

import arrow.core.computations.result
import org.springframework.stereotype.Component
import ventures.dvx.base.user.application.port.`in`.FindUserByIdQuery
import ventures.dvx.base.user.application.port.`in`.FindUserByIdWorkflow
import ventures.dvx.base.user.application.port.`in`.FindUserEvent
import ventures.dvx.base.user.application.port.`in`.UserNotFoundError
import ventures.dvx.base.user.application.port.out.FindUserPort
import ventures.dvx.base.user.application.workflows.mapper.toUserDto
import ventures.dvx.common.workflow.WorkflowDispatcher
import javax.annotation.PostConstruct

@Component
class FindUserByIdWorkflowImpl(
  private val findUserPort: FindUserPort
) : FindUserByIdWorkflow {

  @PostConstruct
  fun registerWithDispatcher() {
    WorkflowDispatcher.registerQueryHandler(this)
  }

  override suspend operator fun invoke(request: FindUserByIdQuery): Result<FindUserEvent> =
    result {
      findUserPort.findUserById(request.userId)
        ?.let { FindUserEvent(it.toUserDto()) }
        ?: throw UserNotFoundError(request.userId)
    }

}
