package ventures.dvx.base.user.application.context

import org.springframework.security.core.userdetails.UserDetails
import org.springframework.stereotype.Component
import ventures.dvx.base.user.application.port.`in`.UserDetailsDto
import ventures.dvx.base.user.application.workflows.SystemFindUserByEmailQuery
import ventures.dvx.base.user.application.workflows.SystemUserFoundEvent
import ventures.dvx.bridgekeeper.Party
import ventures.dvx.bridgekeeper.RoleHandle
import ventures.dvx.bridgekeeper.UserParty
import ventures.dvx.common.logging.LoggerDelegate
import ventures.dvx.common.security.ExecutionContext
import ventures.dvx.common.security.runAsSuperUser
import ventures.dvx.common.workflow.WorkflowDispatcher

@Component
internal class UserContext(
  private val ec: ExecutionContext,
  private val roleHandleMap: Map<String, RoleHandle>,
) {

  val log by LoggerDelegate()

  private fun UserDetails.toParty() =
    UserParty(
      id = username,
      roles = authorities.map {
          ga -> roleHandleMap[ga.authority]!!
      }.toSet()
    )

  suspend fun getCurrentParty(): Party = ec.getCurrentUser().toParty()

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
