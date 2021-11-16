package ventures.dvx.base.web.integration.user

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import ventures.dvx.auth.LoginError
import ventures.dvx.auth.UserLoginInputDto
import ventures.dvx.base.test.BaseWebTest
import ventures.dvx.base.user.adapter.`in`.web.RegisterUserInputDto
import ventures.dvx.base.user.application.port.`in`.RoleDto

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
