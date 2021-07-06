import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFailure
import assertk.assertions.isInstanceOf
import org.junit.jupiter.api.Test
import ventures.dvx.bridgekeeper.ANONYMOUS
import ventures.dvx.bridgekeeper.BridgeKeeper
import ventures.dvx.bridgekeeper.ResourceType
import ventures.dvx.bridgekeeper.RoleHandle
import ventures.dvx.bridgekeeper.UserParty
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
    }.let { BridgeKeeper(it)}

    val adminUser = UserParty("admin", setOf(MyAppRoles.ADMIN))
    val endUserA = UserParty("userA", setOf(MyAppRoles.END_USER))

    assertThat(
      bridgeKeeper.assertCanPerform(adminUser, MY_APPOINTMENT, FakeCommand1::class.simpleName!!)
        .orElseThrow { NullPointerException() }
    ).isEqualTo(Unit)

    assertThat(
      bridgeKeeper.assertCanPerform(endUserA, MY_APPOINTMENT, FakeCommand3::class.simpleName!!)
        .orElseThrow { NullPointerException() }
    ).isEqualTo(Unit)

    assertThat {
      bridgeKeeper.assertCanPerform(ANONYMOUS, MY_APPOINTMENT, FakeCommand1::class.simpleName!!)
        .orElseThrow { NullPointerException() }
    }.isFailure().isInstanceOf(NullPointerException::class)

  }
}
