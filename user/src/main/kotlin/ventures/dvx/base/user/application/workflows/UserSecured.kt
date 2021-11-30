package ventures.dvx.base.user.application.workflows

import ventures.dvx.base.user.application.config.UserBridgekeeperConfig.UserResourceTypes
import ventures.dvx.base.user.application.config.UserBridgekeeperConfig.UserRoles
import ventures.dvx.base.user.application.context.UserContext
import ventures.dvx.bridgekeeper.BridgeKeeper
import ventures.dvx.bridgekeeper.ROLE_SYSTEM_USER
import ventures.dvx.bridgekeeper.ResourceType
import ventures.dvx.bridgekeeper.ResourceTypes
import ventures.dvx.common.ext.className
import ventures.dvx.common.security.UnauthorizedAccessException
import ventures.dvx.common.workflow.Request
import ventures.dvx.common.workflow.Secured

internal interface UserSecured<R: Request> : Secured<R> {

  val bk: BridgeKeeper
  val uc: UserContext

  override suspend fun assertCanPerform(request: R) {
    bk.assertCanPerform(
      uc.getCurrentParty(),
      establishResourceType(request),
      request.className()
    ).orElseThrow { UnauthorizedAccessException() }
  }

  suspend fun userMatchingFn(request: R): Boolean

  suspend fun establishResourceType(request: R): ResourceType =
    with(uc.getCurrentParty().roles) {
      when {
        contains(ROLE_SYSTEM_USER) -> ResourceTypes.SYSTEM
        contains(UserRoles.ROLE_ADMIN_USER) -> UserResourceTypes.ADMIN
        contains(UserRoles.ROLE_ACTIVE_USER) && userMatchingFn(request) -> UserResourceTypes.MY_USER
        contains(UserRoles.ROLE_ACTIVE_USER) && !userMatchingFn(request) -> UserResourceTypes.NOT_MY_USER
        else -> ResourceTypes.EMPTY
      }
    }

}
