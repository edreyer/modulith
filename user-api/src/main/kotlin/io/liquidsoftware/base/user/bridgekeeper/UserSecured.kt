package io.liquidsoftware.base.user.bridgekeeper

import io.liquidsoftware.base.user.bridgekeeper.UserBridgekeeper.getCurrentParty
import io.liquidsoftware.bridgekeeper.BridgeKeeper
import io.liquidsoftware.bridgekeeper.ROLE_SYSTEM_USER
import io.liquidsoftware.bridgekeeper.ResourceType
import io.liquidsoftware.bridgekeeper.ResourceTypes
import io.liquidsoftware.common.ext.className
import io.liquidsoftware.common.security.ExecutionContext
import io.liquidsoftware.common.security.UnauthorizedAccessException
import io.liquidsoftware.common.workflow.Request
import io.liquidsoftware.common.workflow.Secured

interface UserSecured<R: Request> : Secured<R> {

  val bk: BridgeKeeper
  val ec: ExecutionContext

  override suspend fun assertCanPerform(request: R) {
    bk.assertCanPerform(
      ec.getCurrentParty(),
      establishResourceType(request),
      request.className()
    ).orElseThrow { UnauthorizedAccessException("Unauthorized") }
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
