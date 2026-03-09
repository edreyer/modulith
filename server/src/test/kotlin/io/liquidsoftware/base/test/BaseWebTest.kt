package io.liquidsoftware.base.test

import arrow.core.getOrElse
import com.fasterxml.jackson.databind.ObjectMapper
import io.liquidsoftware.base.booking.config.BookingModuleConfig
import io.liquidsoftware.base.payment.config.PaymentModuleConfig
import io.liquidsoftware.base.server.ModulithApplication
import io.liquidsoftware.base.server.config.ServerConfig
import io.liquidsoftware.base.user.adapter.`in`.web.RegisterUserInputDto
import io.liquidsoftware.base.user.adapter.`in`.web.RegisteredUserDto
import io.liquidsoftware.base.user.adapter.`in`.web.SuccessfulLogin
import io.liquidsoftware.base.user.adapter.`in`.web.UserLoginInputDto
import io.liquidsoftware.base.user.application.port.`in`.FindUserApi
import io.liquidsoftware.base.user.application.port.`in`.FindUserByEmailQuery
import io.liquidsoftware.base.user.application.port.`in`.RegisterUserCommand
import io.liquidsoftware.base.user.application.port.`in`.RegisterUserApi
import io.liquidsoftware.base.user.application.port.`in`.RoleDto
import io.liquidsoftware.base.user.application.port.`in`.UserDto
import io.liquidsoftware.common.security.runAsSuperUserBlocking
import io.liquidsoftware.base.user.config.UserModuleConfig
import io.liquidsoftware.common.logging.LoggerDelegate
import io.restassured.RestAssured
import io.restassured.http.ContentType
import io.restassured.response.Response
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestMethodOrder
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.testcontainers.mongodb.MongoDBContainer
import org.testcontainers.utility.DockerImageName

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(SpringExtension::class)
@SpringBootTest(
  classes = [
    ModulithApplication::class,
    ServerConfig::class,
    UserModuleConfig::class,
    BookingModuleConfig::class,
    PaymentModuleConfig::class,
  ],
  webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class BaseWebTest {

  val log by LoggerDelegate()

  @Autowired
  lateinit var objectMapper: ObjectMapper

  @Autowired
  lateinit var registerUserApi: RegisterUserApi

  @Autowired
  lateinit var findUserApi: FindUserApi

  @Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
  @LocalServerPort
  lateinit var port: Integer

  companion object {
    @JvmStatic
    val mongoDBContainer = MongoDBContainer(DockerImageName.parse("mongo:latest"))
      .withReplicaSet()

    @JvmStatic
    @DynamicPropertySource
    fun registerMongoProperties(registry: DynamicPropertyRegistry) {
      mongoDBContainer.start()
      registry.add("spring.mongodb.database") { "test" }
      registry.add("spring.mongodb.uri") { mongoDBContainer.replicaSetUrl }
    }
  }

  protected fun createUser(email: String, msisdn: String): RegisteredUserDto {
    return registerUser(RegisterUserInputDto(
      email = email,
      msisdn = msisdn,
      password = "password",
    ))
  }

  protected fun authorizeUser(email: String, msisdn: String) : SuccessfulLogin = try {
    loginUser(UserLoginInputDto(email, "password"))
  } catch (ex: Throwable) {
    createUser(email, msisdn)
    loginUser(UserLoginInputDto(email, "password"))
  }

  protected fun authorizeAdminUser() : SuccessfulLogin {
    val email = "admin@liquidsoftware.io"
    val password = "password"
    runAsSuperUserBlocking {
      registerUserApi.registerUser(
        RegisterUserCommand(
          msisdn = "5125551234",
          email = email,
          password = password,
          role = RoleDto.ROLE_ADMIN.name
        )
      ).getOrElse { throw it }
    }
    return loginUser(UserLoginInputDto(email, "password"))
  }

  protected fun findUserByEmail(email: String): UserDto =
    runAsSuperUserBlocking {
      findUserApi.findUserByEmail(FindUserByEmailQuery(email))
        .getOrElse { throw it }
        .userDto
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
        this.header("Authorization", "Bearer $token")
      }
    }.get(path)

  protected fun post(path: String, body: Any, token: String? = null): Response =
    RestAssured.given().baseUri("http://localhost:${port}").apply {
      token?.let {
        this.header("Authorization", "Bearer $token")
      }
    }
      .contentType(ContentType.JSON)
      .body(body)
      .post(path)


  protected fun asJson(payload: Any) : String = objectMapper.writeValueAsString(payload)

}
