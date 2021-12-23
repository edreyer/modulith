package ventures.dvx.base.user.bridgekeeper

import ventures.dvx.base.user.bridgekeeper.UserBridgekeeper.getCurrentParty
import ventures.dvx.bridgekeeper.BridgeKeeper
import ventures.dvx.bridgekeeper.ROLE_SYSTEM_USER
import ventures.dvx.bridgekeeper.ResourceType
import ventures.dvx.bridgekeeper.ResourceTypes
import ventures.dvx.common.ext.className
import ventures.dvx.common.security.ExecutionContext
import ventures.dvx.common.security.UnauthorizedAccessException
import ventures.dvx.common.workflow.Request
import ventures.dvx.common.workflow.Secured

interface UserSecured<R: Request> : Secured<R> {

  val bk: BridgeKeeper
  val ec: ExecutionContext

  override suspend fun assertCanPerform(request: R) {
    bk.assertCanPerform(
      ec.getCurrentParty(),
      establishResourceType(request),
      request.className()
    ).orElseThrow { UnauthorizedAccessException() }
  }

  suspend fun userMatchingFn(request: R): Boolean

  suspend fun establishResourceType(request: R): ResourceType =
    with(ec.getCurrentParty().roles) {
      when {
        contains(ROLE_SYSTEM_USER) -> ResourceTypes.SYSTEM
        contains(UserRoles.ROLE_ADMIN_USER) -> UserResourceTypes.ADMIN
        contains(UserRoles.ROLE_ACTIVE_USER) && userMatchingFn(request) -> UserResourceTypes.MY_USER
        contains(UserRoles.ROLE_ACTIVE_USER) && !userMatchingFn(request) -> UserResourceTypes.NOT_MY_USER
        else -> ResourceTypes.EMPTY
      }
    }

}
