package io.liquidsoftware.base.web.integration.user

import io.liquidsoftware.base.test.BaseWebTest
import org.junit.jupiter.api.BeforeAll

class BaseUserWebTest : BaseWebTest() {

  val testEmail = "user@liquidsoftware.io"
  lateinit var accessToken: String

  @BeforeAll
  fun beforeAll() {
    accessToken = authorizeUser(testEmail, "5125550004").accessToken
  }

}
