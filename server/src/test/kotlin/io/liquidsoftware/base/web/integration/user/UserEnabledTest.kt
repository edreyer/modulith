package io.liquidsoftware.base.web.integration.user

import assertk.assertThat
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import io.liquidsoftware.base.test.BaseWebTest
import io.liquidsoftware.base.user.application.port.`in`.FindUserByEmailQuery
import io.liquidsoftware.base.user.application.port.`in`.UserFoundEvent
import io.liquidsoftware.common.security.runAsSuperUser
import io.liquidsoftware.common.workflow.WorkflowDispatcher
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class UserEnabledTest : BaseWebTest() {

  val testEmail = "user@liquidsoftware.io"
  private lateinit var accessToken: String

  @BeforeAll
  fun beforeAll() {
    accessToken = authorizeAdminUser().accessToken
    createUser(testEmail, "5125550004")
  }

  @Test
  fun testUserEnableDisable() = runBlocking {
    val user = runAsSuperUser { WorkflowDispatcher.dispatch<UserFoundEvent>(FindUserByEmailQuery(testEmail)) }
      .getOrThrow().userDto
    assertThat(user.active).isTrue()

    super.get("/api/v1/users/${user.id}/disable", accessToken)
      .then()
      .statusCode(200)

    val disabledUser = runAsSuperUser { WorkflowDispatcher.dispatch<UserFoundEvent>(FindUserByEmailQuery(testEmail)) }
      .getOrThrow().userDto
    assertThat(disabledUser.active).isFalse()

    super.get("/api/v1/users/${user.id}/enable", accessToken)
      .then()
      .statusCode(200)

    val enabledUser = runAsSuperUser { WorkflowDispatcher.dispatch<UserFoundEvent>(FindUserByEmailQuery(testEmail)) }
      .getOrThrow().userDto
    assertThat(enabledUser.active).isTrue()

  }
}
