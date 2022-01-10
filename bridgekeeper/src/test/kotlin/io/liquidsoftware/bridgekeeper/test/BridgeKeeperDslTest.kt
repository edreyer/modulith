package io.liquidsoftware.bridgekeeper.test

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFailure
import assertk.assertions.isInstanceOf
import org.junit.jupiter.api.Test
import io.liquidsoftware.bridgekeeper.ANONYMOUS
import io.liquidsoftware.bridgekeeper.UserParty
import io.liquidsoftware.bridgekeeper.fns.className

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
