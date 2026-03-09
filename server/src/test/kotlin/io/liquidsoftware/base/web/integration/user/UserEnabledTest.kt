package io.liquidsoftware.base.web.integration.user

import assertk.assertThat
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test

class UserEnabledTest : BaseUserWebTest() {

  @Test
  fun testUserEnableDisable() = runBlocking {
    val user = findUserByEmail(testEmail)
    assertThat(user.active).isTrue()

    val admin = authorizeAdminUser()

    get("/api/v1/users/${user.id}/disable", admin.accessToken)
      .then()
      .statusCode(200)

    val disabledUser = findUserByEmail(testEmail)
    assertThat(disabledUser.active).isFalse()

    super.get("/api/v1/users/${user.id}/enable", admin.accessToken)
      .then()
      .statusCode(200)

    val enabledUser = findUserByEmail(testEmail)
    assertThat(enabledUser.active).isTrue()

  }

  @Test
  fun nonAdminCannotDisableAnotherUser() {
    runBlocking {
      val user = findUserByEmail(testEmail)

      get("/api/v1/users/${user.id}/disable", accessToken)
        .then()
        .statusCode(403)
    }
  }
}
