package io.liquidsoftware.base.web.integration.user

import assertk.assertThat
import assertk.assertions.isEqualTo
import io.liquidsoftware.base.test.BaseWebTest
import io.liquidsoftware.base.user.adapter.`in`.web.RegisterUserInputDto
import io.liquidsoftware.base.user.adapter.`in`.web.RegisteredUserDto
import io.liquidsoftware.base.user.application.port.`in`.RoleDto
import io.liquidsoftware.base.user.application.port.`in`.UserDto
import io.liquidsoftware.common.validation.MsisdnParser
import io.restassured.path.json.JsonPath
import io.restassured.response.Response
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test

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
  @Order(1)
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
  @Order(2)
  fun `register existing user`() {
    val regReq = inputDto
    val response: Response = post("/user/register", regReq)
    val json = response.asString()
    val jsonPath = JsonPath(json)
    assertThat(jsonPath.getString("errors")).isEqualTo("User 5125550002 exists")
    assertThat(response.statusCode()).isEqualTo(400)
  }

  @Test
  fun `register blank user`() {
    val regReq = inputDto.copy(msisdn = "", email = "")
    val response: Response = post("/user/register", regReq)
    assertThat(response.statusCode()).isEqualTo(403)
  }

}
