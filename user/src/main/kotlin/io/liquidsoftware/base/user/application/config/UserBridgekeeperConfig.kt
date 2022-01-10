package io.liquidsoftware.base.user.application.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import io.liquidsoftware.base.user.application.port.`in`.DisableUserCommand
import io.liquidsoftware.base.user.application.port.`in`.EnableUserCommand
import io.liquidsoftware.base.user.application.port.`in`.FindUserByEmailQuery
import io.liquidsoftware.base.user.application.port.`in`.FindUserByIdQuery
import io.liquidsoftware.base.user.application.port.`in`.FindUserByMsisdnQuery
import io.liquidsoftware.base.user.application.port.`in`.RegisterUserCommand
import io.liquidsoftware.base.user.application.port.`in`.SystemFindUserByEmailQuery
import io.liquidsoftware.base.user.bridgekeeper.UserResourceTypes.ADMIN
import io.liquidsoftware.base.user.bridgekeeper.UserResourceTypes.MY_USER
import io.liquidsoftware.base.user.bridgekeeper.UserRoles
import io.liquidsoftware.bridgekeeper.BridgeKeeper
import io.liquidsoftware.bridgekeeper.ROLE_SYSTEM_USER
import io.liquidsoftware.bridgekeeper.ResourceTypes
import io.liquidsoftware.bridgekeeper.Visibility
import io.liquidsoftware.bridgekeeper.fns.className
import io.liquidsoftware.bridgekeeper.rolesPermissionRegistry

@Configuration
internal class UserBridgekeeperConfig {

  companion object {
    const val USER_BRIDGE_KEEPER = "userBridgeKeeper"
  }

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
          +className<EnableUserCommand>()
          +className<DisableUserCommand>()
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
