package ventures.dvx.bridgekeeper.test

import ventures.dvx.bridgekeeper.BridgeKeeper
import ventures.dvx.bridgekeeper.ResourceType
import ventures.dvx.bridgekeeper.RoleHandle
import ventures.dvx.bridgekeeper.Visibility
import ventures.dvx.bridgekeeper.rolesPermissionRegistry

object BridgeKeeperTestConfig {

  object MY_APPOINTMENT : ResourceType()
  class FakeCommand1
  class FakeCommand2
  class FakeCommand3

  object MyAppRoles {
    val ADMIN = object: RoleHandle("Admin") {}
    val END_USER = object: RoleHandle("EndUser") {}
  }

  val bridgeKeeper = rolesPermissionRegistry {
    role(MyAppRoles.ADMIN) {
      resourceType(MY_APPOINTMENT) {
        visibility = Visibility.VISIBLE
        operations {
          +FakeCommand1::class.qualifiedName!!
          +FakeCommand2::class.qualifiedName!!
        }
      }
    }
    role(MyAppRoles.END_USER) {
      resourceType(MY_APPOINTMENT) {
        visibility = Visibility.VISIBLE
        operations {
          +FakeCommand3::class.qualifiedName!!
        }
      }
    }
  }.let { BridgeKeeper(it) }

}
