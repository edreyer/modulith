package io.liquidsoftware.base.user.application.workflows

import io.liquidsoftware.base.user.application.config.UserBridgekeeperConfig
import io.liquidsoftware.base.user.application.context.UserContext
import io.liquidsoftware.base.user.application.port.`in`.RoleDto
import io.liquidsoftware.base.user.application.port.`in`.SystemFindUserByEmailQuery
import io.liquidsoftware.base.user.application.port.`in`.SystemUserFoundEvent
import io.liquidsoftware.base.user.application.port.`in`.UserNotFoundError
import io.liquidsoftware.base.user.application.port.out.FindUserPort
import io.liquidsoftware.base.user.bridgekeeper.UserSecured
import io.liquidsoftware.base.user.domain.AdminUser
import io.liquidsoftware.base.user.domain.DisabledUser
import io.liquidsoftware.base.user.domain.User
import io.liquidsoftware.bridgekeeper.BridgeKeeper
import io.liquidsoftware.common.security.UserDetailsWithId
import io.liquidsoftware.common.workflow.BaseSafeSecureWorkflow
import io.liquidsoftware.common.workflow.WorkflowDispatcher
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.stereotype.Component
import javax.annotation.PostConstruct

@Component
internal class SystemFindUserByEmailWorkflow(
  private val findUserPort: FindUserPort,
  uc: UserContext,
  @Qualifier(UserBridgekeeperConfig.USER_BRIDGE_KEEPER) override val bk: BridgeKeeper
) : BaseSafeSecureWorkflow<SystemFindUserByEmailQuery, SystemUserFoundEvent>(),
  UserSecured<SystemFindUserByEmailQuery> {

  override val ec = uc.ec

  @PostConstruct
  fun registerWithDispatcher() = WorkflowDispatcher.registerQueryHandler(this)

  override suspend fun userMatchingFn(request: SystemFindUserByEmailQuery): Boolean = false

  override suspend fun execute(request: SystemFindUserByEmailQuery): SystemUserFoundEvent =
    findUserPort.findUserByEmail(request.email)
      ?.let { SystemUserFoundEvent(it.toUserDetails()) }
      ?: throw UserNotFoundError(request.email)

  private fun User.toUserDetails(): UserDetailsWithId {
    val roles = when (this) {
      is AdminUser -> listOf(RoleDto.ROLE_ADMIN)
      else -> listOf(RoleDto.ROLE_USER)
    }
    return when (this) {
      is DisabledUser -> UserDetailsWithId(
        id = this.id.value,
        user = org.springframework.security.core.userdetails.User(
          this.email.value,
          this.encryptedPassword.value,
          false, false, false, false,
          listOf()
        )
      )
      else -> UserDetailsWithId(
        id = this.id.value,
        user = org.springframework.security.core.userdetails.User(
          this.email.value,
          this.encryptedPassword.value,
          true, true, true, true,
          roles.map { SimpleGrantedAuthority(it.name) }
        )
      )
    }
  }

}

