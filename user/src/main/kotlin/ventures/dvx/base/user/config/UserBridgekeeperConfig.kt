package ventures.dvx.base.user.config

import org.springframework.context.annotation.Configuration
import ventures.dvx.base.user.domain.ActiveUser
import ventures.dvx.base.user.domain.AdminUser
import ventures.dvx.bridgekeeper.ResourceType
import ventures.dvx.bridgekeeper.RoleHandle
import ventures.dvx.bridgekeeper.fns.className

// TODO: Configure Bridgekeeper
@Configuration
class UserBridgekeeperConfig {

  object UserRoles {
    val ROLE_ADMIN = object: RoleHandle(className<AdminUser>()) {}
    val ROLE_END_USER = object: RoleHandle(className<ActiveUser>()) {}
  }

  object ResourceTypes {
    val ADMIN = object : ResourceType() {}
    val NOT_ADMIN = object : ResourceType() {}
    val MY_USER = object : ResourceType() {}
    val NOT_MY_USER = object : ResourceType() {}
  }

  // Bridgekeeper
//  @Bean("userBridgeKeeper")
//  fun userBridgeKeeper() = rolesPermissionRegistry {
//    role(UserRoles.ROLE_ADMIN) {
//      resourceType(MY_USER) {
//        visibility = Visibility.VISIBLE
//        operations {
//          +className<RegisterAdminUserCommand>()
//          +className<FindUserByIdQuery>()
//          +className<FindUserByUsernameQuery>()
//        }
//      }
//      resourceType(NOT_MY_USER) {
//        visibility = Visibility.VISIBLE
//        operations {
//          +className<RegisterAdminUserCommand>()
//          +className<FindUserByIdQuery>()
//          +className<FindUserByUsernameQuery>()
//        }
//      }
//    }
//    role(UserRoles.ROLE_END_USER) {
//      resourceType(MY_USER) {
//        visibility = Visibility.VISIBLE
//        operations {
//          +className<FindUserByIdQuery>()
//          +className<FindUserByUsernameQuery>()
//        }
//      }
//    }
//    role(ROLE_SYSTEM) {
//      resourceType(NOT_MY_USER) {
//        visibility = Visibility.VISIBLE
//        operations {
//          +className<RegisterAdminUserCommand>()
//          +className<FindUserByIdQuery>()
//          +className<FindUserByUsernameQuery>()
//        }
//      }
//    }
//  }.let { BridgeKeeper(it) }

}
