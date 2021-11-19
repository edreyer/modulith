package ventures.dvx.base.user.application.workflows

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.stereotype.Component
import ventures.dvx.base.user.application.config.UserBridgekeeperConfig
import ventures.dvx.base.user.application.context.UserContext
import ventures.dvx.base.user.application.port.`in`.RoleDto
import ventures.dvx.base.user.application.port.`in`.SystemFindUserByEmailQuery
import ventures.dvx.base.user.application.port.`in`.SystemFindUserByEmailWorkflow
import ventures.dvx.base.user.application.port.`in`.SystemUserFoundEvent
import ventures.dvx.base.user.application.port.`in`.UserDetailsDto
import ventures.dvx.base.user.application.port.`in`.UserNotFoundError
import ventures.dvx.base.user.application.port.out.FindUserPort
import ventures.dvx.base.user.domain.AdminUser
import ventures.dvx.base.user.domain.DisabledUser
import ventures.dvx.base.user.domain.User
import ventures.dvx.bridgekeeper.BridgeKeeper
import ventures.dvx.common.workflow.WorkflowDispatcher
import javax.annotation.PostConstruct

@Component
internal class SystemFindUserByEmailWorkflowImpl(
  private val findUserPort: FindUserPort,
  override val uc: UserContext,
  @Qualifier(UserBridgekeeperConfig.USER_BRIDGE_KEEPER) override val bk: BridgeKeeper
) : SystemFindUserByEmailWorkflow() {

  @PostConstruct
  fun registerWithDispatcher() {
    WorkflowDispatcher.registerQueryHandler(this)
  }

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

