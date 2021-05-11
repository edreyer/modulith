package ventures.dvx.test

import com.fasterxml.jackson.databind.ObjectMapper
import io.restassured.RestAssured
import io.restassured.http.ContentType
import io.restassured.response.Response
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.web.server.LocalServerPort
import org.springframework.test.context.junit.jupiter.SpringExtension
import ventures.dvx.base.user.adapter.`in`.web.RegisterUserInputDto
import ventures.dvx.base.user.adapter.`in`.web.RegisteredUserDto

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(SpringExtension::class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class BaseWebTest {

  @Autowired
  lateinit var objectMapper: ObjectMapper

  @LocalServerPort
  lateinit var port: Integer


  protected fun registerUser(newUser: RegisterUserInputDto) =
    post("/user/register", newUser)
      .then()
      .statusCode(200)
      .extract().`as`(RegisteredUserDto::class.java)


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