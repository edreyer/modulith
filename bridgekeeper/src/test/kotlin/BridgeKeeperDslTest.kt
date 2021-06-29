import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFailure
import assertk.assertions.isInstanceOf
import org.junit.jupiter.api.Test
import ventures.dvx.bridgekeeper.ANONYMOUS
import ventures.dvx.bridgekeeper.BridgeKeeper
import ventures.dvx.bridgekeeper.ResourceType
import ventures.dvx.bridgekeeper.RoleHandle
import ventures.dvx.bridgekeeper.User
import ventures.dvx.bridgekeeper.Visibility

class BridgeKeeperDslTest {

  object MY_APPOINTMENT : ResourceType()
  class FakeCommand1
  class FakeCommand2
  class FakeCommand3

  object MyAppRoles {
    val ADMIN = object: RoleHandle("Admin") {}
    val END_USER = object: RoleHandle("EndUser") {}
  }

  @Test
  fun canUseDsl() {

    val bridgeKeeper = rolesPermissionRegistry {
      role(MyAppRoles.ADMIN) {
        resourceType(MY_APPOINTMENT) {
          visibility = Visibility.VISIBLE
          operations {
            +FakeCommand1::class
            +FakeCommand2::class
            +FakeCommand3::class
          }
        }
      }
      role(MyAppRoles.END_USER) {
        resourceType(MY_APPOINTMENT) {
          visibility = Visibility.VISIBLE
          operations {
            +FakeCommand1::class
          }
        }
      }
    }.let { BridgeKeeper(it)}

    val adminUser = User("admin", setOf(MyAppRoles.ADMIN))
    val endUserA = User("userA", setOf(MyAppRoles.END_USER))
    val endUserB = User("userB", setOf(MyAppRoles.END_USER))

    assertThat(
      bridgeKeeper.assertCanPerform(adminUser, MY_APPOINTMENT, FakeCommand1::class)
        .orElseThrow { NullPointerException() }
    ).isEqualTo(Unit)

    assertThat(
      bridgeKeeper.assertCanPerform(endUserA, MY_APPOINTMENT, FakeCommand1::class)
        .orElseThrow { NullPointerException() }
    ).isEqualTo(Unit)

    assertThat(
      bridgeKeeper.assertCanPerform(endUserB, MY_APPOINTMENT, FakeCommand1::class)
        .orElseThrow { NullPointerException() }
    ).isEqualTo(Unit)

    assertThat {
      bridgeKeeper.assertCanPerform(ANONYMOUS, MY_APPOINTMENT, FakeCommand1::class)
        .orElseThrow { NullPointerException() }
    }.isFailure().isInstanceOf(NullPointerException::class)

  }
}
