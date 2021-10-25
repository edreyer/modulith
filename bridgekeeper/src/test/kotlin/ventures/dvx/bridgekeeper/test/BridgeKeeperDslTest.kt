package ventures.dvx.bridgekeeper.test

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFailure
import assertk.assertions.isInstanceOf
import org.junit.jupiter.api.Test
import ventures.dvx.bridgekeeper.ANONYMOUS
import ventures.dvx.bridgekeeper.UserParty
import ventures.dvx.bridgekeeper.fns.className

class BridgeKeeperDslTest {


  @Test
  fun canUseDsl() {

    val bridgeKeeper = BridgeKeeperTestConfig.bridgeKeeper

    val adminUser = UserParty("admin", setOf(BridgeKeeperTestConfig.MyAppRoles.ADMIN))
    val endUserA = UserParty("userA", setOf(BridgeKeeperTestConfig.MyAppRoles.END_USER))

    assertThat(
      bridgeKeeper.assertCanPerform(adminUser,
        BridgeKeeperTestConfig.MY_APPOINTMENT, className<BridgeKeeperTestConfig.FakeCommand1>())
        .orElseThrow { NullPointerException() }
    ).isEqualTo(Unit)

    assertThat(
      bridgeKeeper.assertCanPerform(endUserA,
        BridgeKeeperTestConfig.MY_APPOINTMENT, className<BridgeKeeperTestConfig.FakeCommand3>())
        .orElseThrow { NullPointerException() }
    ).isEqualTo(Unit)

    assertThat {
      bridgeKeeper.assertCanPerform(ANONYMOUS,
        BridgeKeeperTestConfig.MY_APPOINTMENT, className<BridgeKeeperTestConfig.FakeCommand1>())
        .orElseThrow { NullPointerException() }
    }.isFailure().isInstanceOf(NullPointerException::class)

  }
}
