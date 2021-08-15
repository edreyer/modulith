package ventures.dvx.test

import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.junit.jupiter.SpringExtension

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(SpringExtension::class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class BaseWebTest {

//  @Autowired
//  lateinit var objectMapper: ObjectMapper
//
//  @LocalServerPort
//  lateinit var port: Integer
//
//  protected fun registerUser(newUser: RegisterUserInputDto) =
//    post("/user/register", newUser)
//      .then()
//      .statusCode(200)
//      .extract().`as`(RegisteredUserDto::class.java)
//
//
//  protected fun get(path: String, token: String? = null): Response =
//    RestAssured.given().baseUri("http://localhost:${port}").apply {
//      token?.let {
//        this.header("Authorization", "Bearer ${token}")
//      }
//    }.get(path)
//
//  protected fun post(path: String, body: Any): Response =
//    RestAssured.given().baseUri("http://localhost:${port}").contentType(ContentType.JSON).body(body).post(path)
//
//
//  protected fun asJson(payload: Any) : String = objectMapper.writeValueAsString(payload)

}
