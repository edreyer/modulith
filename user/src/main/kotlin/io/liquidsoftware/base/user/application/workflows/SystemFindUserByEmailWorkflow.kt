package io.liquidsoftware.base.user.application.workflows

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.stereotype.Component
import io.liquidsoftware.base.user.application.config.UserBridgekeeperConfig
import io.liquidsoftware.base.user.application.context.UserContext
import io.liquidsoftware.base.user.application.port.`in`.RoleDto
import io.liquidsoftware.base.user.application.port.`in`.SystemFindUserByEmailQuery
import io.liquidsoftware.base.user.application.port.`in`.SystemUserFoundEvent
import io.liquidsoftware.base.user.application.port.`in`.UserDetailsDto
import io.liquidsoftware.base.user.application.port.`in`.UserNotFoundError
import io.liquidsoftware.base.user.application.port.out.FindUserPort
import io.liquidsoftware.base.user.bridgekeeper.UserSecured
import io.liquidsoftware.base.user.domain.AdminUser
import io.liquidsoftware.base.user.domain.DisabledUser
import io.liquidsoftware.base.user.domain.User
import io.liquidsoftware.bridgekeeper.BridgeKeeper
import io.liquidsoftware.common.workflow.BaseSafeSecureWorkflow
import io.liquidsoftware.common.workflow.WorkflowDispatcher
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
      ?.let { SystemUserFoundEvent(it.toUserForSystemDto()) }
      ?: throw UserNotFoundError(request.email)

  private fun User.toUserForSystemDto(): UserDetailsDto {
    val roles = when (this) {
      is AdminUser -> listOf(RoleDto.ROLE_ADMIN)
      else -> listOf(RoleDto.ROLE_USER)
    }
    val userDetails = org.springframework.security.core.userdetails.User(
      this.email.value,
      this.encryptedPassword.value,
      roles.map { SimpleGrantedAuthority(it.name) }
    )
    return when (this) {
      is DisabledUser -> UserDetailsDto(
        userDetails = userDetails,
        id = this.id.value,
        email = this.email.value,
        msisdn = this.msisdn.value,
        active = false,
        roles = roles
      )
      else -> UserDetailsDto(
        userDetails = userDetails,
        id = this.id.value,
        email = this.email.value,
        msisdn = this.msisdn.value,
        active = true,
        roles = roles
      )
    }
  }

}

