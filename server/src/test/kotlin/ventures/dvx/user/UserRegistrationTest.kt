package ventures.dvx.user

import io.restassured.path.json.JsonPath
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import ventures.dvx.base.user.adapter.`in`.web.RegisterUserInputDto
import ventures.dvx.base.user.adapter.`in`.web.RegisteredUserDto
import ventures.dvx.base.user.application.port.`in`.RoleDto
import ventures.dvx.base.user.application.port.`in`.UserDto
import ventures.dvx.common.validation.MsisdnParser
import ventures.dvx.test.BaseWebTest

class UserRegistrationTest : BaseWebTest() {

  private lateinit var inputDto: RegisterUserInputDto

  @BeforeAll
  fun initUser() {
    inputDto = RegisterUserInputDto(
      msisdn = "5125550002",
      email = "foo@bar.com",
      password = "password",
      role = RoleDto.ROLE_USER
    )
  }

  @Test
  fun `register new user`() {
    val regReq = inputDto
    val actual = registerUser(regReq)
    val expected = RegisteredUserDto(UserDto(
      id = actual.user.id,
      email = inputDto.email,
      msisdn = MsisdnParser.toInternational(inputDto.msisdn),
      active = true,
      roles = listOf(RoleDto.ROLE_USER)
    ))
    assertThat(actual).isEqualTo(expected)
  }

  @Test
  fun `register invalid user`() {
    val regReq = inputDto.copy(msisdn = "", email = "")
    val json = post("/user/register", regReq).asString()
    val jsonPath = JsonPath(json)
    assertThat(jsonPath.getInt("status")).isEqualTo(400)
    assertThat(jsonPath.getString("error")).isEqualTo("Bad Request")
  }


}
