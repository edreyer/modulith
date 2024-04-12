package io.liquidsoftware.base.web.integration.user

import assertk.assertThat
import assertk.assertions.isNotEmpty
import io.liquidsoftware.base.test.BaseWebTest
import io.liquidsoftware.base.user.adapter.`in`.web.RegisterUserInputDto
import io.liquidsoftware.base.user.adapter.`in`.web.UserLoginInputDto
import io.liquidsoftware.base.user.application.port.`in`.RoleDto
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class AuthenticationTest : BaseWebTest() {

  private lateinit var inputDto: UserLoginInputDto

  val testData = RegisterUserInputDto(
    msisdn = "5125550001",
    email = "bob@loblaw.com",
    password = "password",
    role = RoleDto.ROLE_USER
  )

  @BeforeAll
  fun initUser() {
    inputDto = UserLoginInputDto(
      username = testData.email,
      password = testData.password
    )
  }

  @Test
  fun `successful login`() {
    registerUser(testData)
    assertThat(loginUser(inputDto).accessToken).isNotEmpty()
  }

  @Test
  fun `unsuccessful login`() {
    post(
      "/auth/login",
      UserLoginInputDto("foo", "bar")
    )
      .then()
      .statusCode(403)
  }

}
