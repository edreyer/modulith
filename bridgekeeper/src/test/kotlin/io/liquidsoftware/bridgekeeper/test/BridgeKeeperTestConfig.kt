package io.liquidsoftware.bridgekeeper.test

import io.liquidsoftware.bridgekeeper.BridgeKeeper
import io.liquidsoftware.bridgekeeper.ResourceType
import io.liquidsoftware.bridgekeeper.RoleHandle
import io.liquidsoftware.bridgekeeper.Visibility
import io.liquidsoftware.bridgekeeper.rolesPermissionRegistry

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
