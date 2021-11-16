package ventures.dvx.base.web.integration.user

import assertk.assertThat
import assertk.assertions.isEqualTo
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import ventures.dvx.base.test.BaseWebTest
import ventures.dvx.base.user.adapter.`in`.web.api.v1.FoundUserUserOutputDto

class FindUserTest : BaseWebTest() {

  val testEmail = "findUser@dvx.ventures"
  private lateinit var accessToken: String

  @BeforeAll
  fun beforeAll() {
    accessToken = authorizeUser(testEmail, "5125550003").accessToken
  }

  @Test
  fun testNotAuthorized() {
    // no auth token
    this.get("/api/v1/users/email/$testEmail")
      .then()
      .statusCode(401)
  }

  @Test
  fun testFindMyUserByEmail() {
    val actual = this.get("/api/v1/users/email/$testEmail", accessToken)
      .then()
      .statusCode(200)
      .extract().`as`(FoundUserUserOutputDto::class.java)
    assertThat(actual.userDto.email).isEqualTo(testEmail)
  }

  @Test
  fun testFindNotMyUserByEmail() {
    this.get("/api/v1/users/email/notMyEmail@foo.com", accessToken)
      .then()
      .statusCode(400)
  }

}
