package ventures.dvx.base.user.application.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import ventures.dvx.base.user.application.config.UserBridgekeeperConfig.UserResourceTypes.ADMIN
import ventures.dvx.base.user.application.config.UserBridgekeeperConfig.UserResourceTypes.MY_USER
import ventures.dvx.base.user.application.port.`in`.FindUserByEmailQuery
import ventures.dvx.base.user.application.port.`in`.FindUserByIdQuery
import ventures.dvx.base.user.application.port.`in`.FindUserByMsisdnQuery
import ventures.dvx.base.user.application.port.`in`.RegisterUserCommand
import ventures.dvx.base.user.application.port.`in`.SystemFindUserByEmailQuery
import ventures.dvx.bridgekeeper.BridgeKeeper
import ventures.dvx.bridgekeeper.ROLE_SYSTEM_USER
import ventures.dvx.bridgekeeper.ResourceType
import ventures.dvx.bridgekeeper.ResourceTypes
import ventures.dvx.bridgekeeper.RoleHandle
import ventures.dvx.bridgekeeper.Visibility
import ventures.dvx.bridgekeeper.fns.className
import ventures.dvx.bridgekeeper.rolesPermissionRegistry

@Configuration
internal class UserBridgekeeperConfig {

  companion object {
    const val USER_BRIDGE_KEEPER = "userBridgeKeeper"
  }

  object UserRoles {
    val ROLE_ADMIN_USER = object: RoleHandle("ROLE_ADMIN") {}
    val ROLE_ACTIVE_USER = object: RoleHandle("ROLE_USER") {}
  }

  object UserResourceTypes {
    val ADMIN = object : ResourceType() {}
    val MY_USER = object : ResourceType() {}
    val NOT_MY_USER = object : ResourceType() {}
  }

  @Bean("roleHandleMap")
  fun roleHandleMap() = mapOf(
    ROLE_SYSTEM_USER.name to ROLE_SYSTEM_USER,
    UserRoles.ROLE_ADMIN_USER.name to UserRoles.ROLE_ADMIN_USER,
    UserRoles.ROLE_ACTIVE_USER.name to UserRoles.ROLE_ACTIVE_USER
  )

  // Bridgekeeper
  @Bean(USER_BRIDGE_KEEPER)
  fun userBridgeKeeper() = rolesPermissionRegistry {
    role(ROLE_SYSTEM_USER) {
      resourceType(ResourceTypes.SYSTEM) {
        visibility = Visibility.VISIBLE
        operations {
          +className<RegisterUserCommand>()
          +className<SystemFindUserByEmailQuery>()
          +className<FindUserByIdQuery>()
          +className<FindUserByEmailQuery>()
          +className<FindUserByMsisdnQuery>()
        }
      }
    }
    role(UserRoles.ROLE_ADMIN_USER) {
      resourceType(ADMIN) {
        visibility = Visibility.VISIBLE
        operations {
          +className<RegisterUserCommand>()
          +className<FindUserByIdQuery>()
          +className<FindUserByEmailQuery>()
          +className<FindUserByMsisdnQuery>()
        }
      }
    }
    role(UserRoles.ROLE_ACTIVE_USER) {
      resourceType(MY_USER) {
        visibility = Visibility.VISIBLE
        operations {
          +className<FindUserByIdQuery>()
          +className<FindUserByEmailQuery>()
          +className<FindUserByMsisdnQuery>()
        }
      }
    }
  }.let { BridgeKeeper(it) }

}
