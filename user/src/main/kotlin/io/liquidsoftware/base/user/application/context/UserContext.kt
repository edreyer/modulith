package io.liquidsoftware.base.user.application.context

import org.springframework.stereotype.Component
import io.liquidsoftware.base.user.application.port.`in`.SystemFindUserByEmailQuery
import io.liquidsoftware.base.user.application.port.`in`.SystemUserFoundEvent
import io.liquidsoftware.base.user.application.port.`in`.UserDetailsDto
import io.liquidsoftware.common.logging.LoggerDelegate
import io.liquidsoftware.common.security.ExecutionContext
import io.liquidsoftware.common.security.runAsSuperUser
import io.liquidsoftware.common.workflow.WorkflowDispatcher

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
