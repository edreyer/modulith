package io.liquidsoftware.base.user.application.context

import io.liquidsoftware.base.user.application.port.`in`.FindUserByIdQuery
import io.liquidsoftware.base.user.application.port.`in`.UserDto
import io.liquidsoftware.base.user.application.port.`in`.UserFoundEvent
import io.liquidsoftware.common.logging.LoggerDelegate
import io.liquidsoftware.common.security.ExecutionContext
import io.liquidsoftware.common.security.runAsSuperUser
import io.liquidsoftware.common.workflow.WorkflowDispatcher
import org.springframework.stereotype.Component

@Component
internal class UserContext(
  val ec: ExecutionContext
) {

  val log by LoggerDelegate()

  suspend fun getCurrentUser(): UserDto {
    val userId = ec.getCurrentUser().id
    return runAsSuperUser {
      WorkflowDispatcher.dispatch<UserFoundEvent>(FindUserByIdQuery(userId))
        .fold(
          { it.userDto },
          { ex ->
            log.error("Failed to get current user", ex)
            throw ex
          }
        )
    }
  }

}
