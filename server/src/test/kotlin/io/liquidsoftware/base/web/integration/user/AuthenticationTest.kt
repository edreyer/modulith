package io.liquidsoftware.base.web.integration.user

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import io.liquidsoftware.base.test.BaseWebTest
import io.liquidsoftware.base.user.adapter.`in`.web.LoginError
import io.liquidsoftware.base.user.adapter.`in`.web.RegisterUserInputDto
import io.liquidsoftware.base.user.adapter.`in`.web.UserLoginInputDto
import io.liquidsoftware.base.user.application.port.`in`.RoleDto

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
    assertThat(loginUser(inputDto).accessToken).isNotBlank
  }

  @Test
  fun `unsuccessful login`() {
    val actual = post(
      "/auth/login",
      UserLoginInputDto("foo", "bar")
    )
      .then()
      .statusCode(400)
      .extract().`as`(LoginError::class.java)
    assertThat(actual.err).isNotBlank
  }

}