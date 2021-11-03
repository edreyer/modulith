package ventures.dvx.user

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import ventures.dvx.base.user.adapter.`in`.web.RegisterUserErrorsDto
import ventures.dvx.base.user.adapter.`in`.web.RegisterUserInputDto
import ventures.dvx.base.user.adapter.`in`.web.RegisteredUserDto
import ventures.dvx.test.BaseWebTest

class UserRegistrationTest : BaseWebTest() {

  private lateinit var inputDto: RegisterUserInputDto

  @BeforeAll
  fun initUser() {
    inputDto = RegisterUserInputDto(
      username = "foo",
      email = "foo@bar.com",
      password = "password"
    )
  }

  @Test
  fun `register new user`() {
    val regReq = inputDto
    val expected = RegisteredUserDto(
      username = inputDto.username,
      email = inputDto.email
    )
    val actual = registerUser(regReq)
    assertThat(actual).isEqualTo(expected)
  }

  @Test
  fun `register invalid user`() {
    val regReq = inputDto.copy(username = "", email = "")
    val expected = RegisterUserErrorsDto(
      errors = listOf(
        """'' of NonEmptyString.value: Must not be empty""",
        """'' of EmailAddress.value: Must not be empty""",
        """'' of EmailAddress.value: Must be a valid email address"""
      )
    )
    val actual = post("/user/register", regReq)
      .then()
      .statusCode(400)
      .extract().`as`(RegisterUserErrorsDto::class.java)
    assertThat(actual).isEqualTo(expected)
  }


}
