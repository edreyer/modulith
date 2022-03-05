package io.liquidsoftware.base.test

import com.fasterxml.jackson.databind.ObjectMapper
import io.liquidsoftware.base.server.ModulithApplication
import io.liquidsoftware.base.server.config.ServerConfig
import io.liquidsoftware.base.user.adapter.`in`.web.RegisterUserInputDto
import io.liquidsoftware.base.user.adapter.`in`.web.RegisteredUserDto
import io.liquidsoftware.base.user.adapter.`in`.web.SuccessfulLogin
import io.liquidsoftware.base.user.adapter.`in`.web.UserLoginInputDto
import io.liquidsoftware.base.user.application.port.`in`.RoleDto
import io.liquidsoftware.base.user.config.UserModuleConfig
import io.liquidsoftware.common.logging.LoggerDelegate
import io.restassured.RestAssured
import io.restassured.http.ContentType
import io.restassured.response.Response
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.web.server.LocalServerPort
import org.springframework.test.context.junit.jupiter.SpringExtension

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(SpringExtension::class)
@SpringBootTest(
  classes = [
    ModulithApplication::class,
    ServerConfig::class,
    UserModuleConfig::class
  ],
  webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class BaseWebTest {

  val log by LoggerDelegate()

  @Autowired
  lateinit var objectMapper: ObjectMapper

  @Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
  @LocalServerPort
  lateinit var port: Integer

  protected fun createUser(email: String, msisdn: String): RegisteredUserDto {
    return registerUser(RegisterUserInputDto(
      email = email,
      msisdn = msisdn,
      password = "password",
      role = RoleDto.ROLE_USER
    ))
  }

  protected fun authorizeUser(email: String, msisdn: String) : SuccessfulLogin {
    createUser(email, msisdn)
    return loginUser(UserLoginInputDto(email, "password"))
  }

  protected fun authorizeAdminUser() : SuccessfulLogin {
    val email = "admin@liquidsoftware.io"
    val password = "password"
    registerUser(RegisterUserInputDto(
      email = email,
      msisdn = "5125551234",
      password = password,
      role = RoleDto.ROLE_ADMIN
    ))
    return loginUser(UserLoginInputDto(email, "password"))
  }

  protected fun registerUser(newUser: RegisterUserInputDto): RegisteredUserDto {
    log.info("Registering User with email: ${newUser.email}")
    return post("/user/register", newUser)
      .then()
      .statusCode(200)
      .extract().`as`(RegisteredUserDto::class.java)
  }

  protected fun loginUser(loginDto: UserLoginInputDto): SuccessfulLogin {
    log.info("Authenticating User with email ${loginDto.username}")
    return post("/auth/login", loginDto)
      .then()
      .statusCode(200)
      .extract().`as`(SuccessfulLogin::class.java)
  }

  protected fun get(path: String, token: String? = null): Response =
    RestAssured.given().baseUri("http://localhost:${port}").apply {
      token?.let {
        this.header("Authorization", "Bearer ${token}")
      }
    }.get(path)

  protected fun post(path: String, body: Any): Response =
    RestAssured.given().baseUri("http://localhost:${port}").contentType(ContentType.JSON).body(body).post(path)


  protected fun asJson(payload: Any) : String = objectMapper.writeValueAsString(payload)

}
