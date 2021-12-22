package ventures.dvx.base.user.config.bridgekeeper

import org.springframework.security.core.userdetails.UserDetails
import ventures.dvx.bridgekeeper.Party
import ventures.dvx.bridgekeeper.ROLE_SYSTEM_USER
import ventures.dvx.bridgekeeper.ResourceType
import ventures.dvx.bridgekeeper.RoleHandle
import ventures.dvx.bridgekeeper.UserParty
import ventures.dvx.common.security.ExecutionContext

object UserRoles {
  val ROLE_ADMIN_USER = object: RoleHandle("ROLE_ADMIN") {}
  val ROLE_ACTIVE_USER = object: RoleHandle("ROLE_USER") {}
}

object UserResourceTypes {
  val ADMIN = object : ResourceType() {}
  val MY_USER = object : ResourceType() {}
  val NOT_MY_USER = object : ResourceType() {}
}

object UserBridgekeeper {
  val userRoleHandleMap = mapOf(
    ROLE_SYSTEM_USER.name to ROLE_SYSTEM_USER,
    UserRoles.ROLE_ADMIN_USER.name to UserRoles.ROLE_ADMIN_USER,
    UserRoles.ROLE_ACTIVE_USER.name to UserRoles.ROLE_ACTIVE_USER
  )

  private fun UserDetails.toParty() =
    UserParty(
      id = username,
      roles = authorities.map {
          ga -> userRoleHandleMap[ga.authority]!!
      }.toSet()
    )

  suspend fun ExecutionContext.getCurrentParty(): Party = this.getCurrentUser().toParty()

}
