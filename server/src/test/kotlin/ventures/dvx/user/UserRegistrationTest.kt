package ventures.dvx.user

import io.restassured.path.json.JsonPath
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import ventures.dvx.base.user.adapter.`in`.web.RegisterUserErrorsDto
import ventures.dvx.base.user.adapter.`in`.web.RegisterUserInputDto
import ventures.dvx.base.user.adapter.`in`.web.RegisteredUserDto
import ventures.dvx.common.validation.MsisdnParser
import ventures.dvx.test.BaseWebTest

class UserRegistrationTest : BaseWebTest() {

  private lateinit var inputDto: RegisterUserInputDto

  @BeforeAll
  fun initUser() {
    inputDto = RegisterUserInputDto(
      msisdn = "5125550001",
      email = "foo@bar.com",
      password = "password"
    )
  }

  @Test
  fun `register new user`() {
    val regReq = inputDto
    val expected = RegisteredUserDto(
      msisdn = MsisdnParser.toInternational(inputDto.msisdn),
      email = inputDto.email
    )
    val actual = registerUser(regReq)
    assertThat(actual).isEqualTo(expected)
  }

  @Test
  fun `register invalid user`() {
    val regReq = inputDto.copy(msisdn = "", email = "")
    val expected = RegisterUserErrorsDto(
      errors = listOf(
        """'' of Msisdn.value: Must be valid""",
        """'' of EmailAddress.value: Must not be empty""",
        """'' of EmailAddress.value: Must be a valid email address"""
      )
    )
    val json = post("/user/register", regReq).asString()
    val jsonPath = JsonPath(json)
    assertThat(jsonPath.getInt("status")).isEqualTo(400)
    assertThat(jsonPath.getString("error")).isEqualTo("Bad Request")
  }


}
