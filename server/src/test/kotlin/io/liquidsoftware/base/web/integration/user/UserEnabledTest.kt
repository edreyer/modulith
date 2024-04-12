package io.liquidsoftware.base.web.integration.user

import arrow.core.getOrElse
import assertk.assertThat
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import io.liquidsoftware.base.user.application.port.`in`.FindUserByEmailQuery
import io.liquidsoftware.base.user.application.port.`in`.UserFoundEvent
import io.liquidsoftware.common.ext.toResult
import io.liquidsoftware.common.security.runAsSuperUser
import io.liquidsoftware.common.workflow.WorkflowDispatcher
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test

class UserEnabledTest : BaseUserWebTest() {

  @Test
  fun testUserEnableDisable() = runBlocking {
    val user = runAsSuperUser {
      WorkflowDispatcher.dispatch<UserFoundEvent>(FindUserByEmailQuery(testEmail))
        .getOrElse { throw it }
        .userDto
    }
    assertThat(user.active).isTrue()

    val admin = authorizeAdminUser()

    get("/api/v1/users/${user.id}/disable", admin.accessToken)
      .then()
      .statusCode(200)

    val disabledUser = runAsSuperUser {
      WorkflowDispatcher.dispatch<UserFoundEvent>(FindUserByEmailQuery(testEmail))
        .getOrElse { throw it }
        .userDto
    }
    assertThat(disabledUser.active).isFalse()

    super.get("/api/v1/users/${user.id}/enable", admin.accessToken)
      .then()
      .statusCode(200)

    val enabledUser = runAsSuperUser {
      WorkflowDispatcher.dispatch<UserFoundEvent>(FindUserByEmailQuery(testEmail)).toResult()
    }
      .getOrThrow().userDto
    assertThat(enabledUser.active).isTrue()

  }
}
