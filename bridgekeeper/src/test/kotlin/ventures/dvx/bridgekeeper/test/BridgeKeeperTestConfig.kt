package ventures.dvx.bridgekeeper.test

import rolesPermissionRegistry
import ventures.dvx.bridgekeeper.BridgeKeeper
import ventures.dvx.bridgekeeper.ResourceType
import ventures.dvx.bridgekeeper.RoleHandle
import ventures.dvx.bridgekeeper.Visibility

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
          +FakeCommand1::class.simpleName!!
          +FakeCommand2::class.simpleName!!
        }
      }
    }
    role(MyAppRoles.END_USER) {
      resourceType(MY_APPOINTMENT) {
        visibility = Visibility.VISIBLE
        operations {
          +FakeCommand3::class.simpleName!!
        }
      }
    }
  }.let { BridgeKeeper(it) }

}
