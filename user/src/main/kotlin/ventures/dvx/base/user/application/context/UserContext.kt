package ventures.dvx.base.user.application.context

import org.springframework.stereotype.Component
import ventures.dvx.base.user.application.port.`in`.SystemFindUserByEmailQuery
import ventures.dvx.base.user.application.port.`in`.SystemUserFoundEvent
import ventures.dvx.base.user.application.port.`in`.UserDetailsDto
import ventures.dvx.common.logging.LoggerDelegate
import ventures.dvx.common.security.ExecutionContext
import ventures.dvx.common.security.runAsSuperUser
import ventures.dvx.common.workflow.WorkflowDispatcher

@Component
internal class UserContext(
  val ec: ExecutionContext
) {

  val log by LoggerDelegate()

  suspend fun getCurrentUser(): UserDetailsDto {
    val currentUser = ec.getCurrentUser().username
    return runAsSuperUser {
      WorkflowDispatcher.dispatch<SystemUserFoundEvent>(SystemFindUserByEmailQuery(currentUser))
        .fold(
          { it.userDetailsDto },
          { ex ->
            log.error("Failed to get current user", ex)
            throw ex
          }
        )
    }
  }

}
