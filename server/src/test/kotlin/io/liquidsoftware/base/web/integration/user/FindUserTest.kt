package io.liquidsoftware.base.web.integration.user

import assertk.assertThat
import assertk.assertions.isEqualTo
import io.liquidsoftware.base.test.BaseWebTest
import io.liquidsoftware.base.user.adapter.`in`.web.api.v1.FoundUserUserOutputDto
import io.liquidsoftware.base.user.adapter.`in`.web.api.v1.RegisterUserErrorsDto
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class FindUserTest : BaseWebTest() {

  val testEmail = "findUser@liquidsoftware.io"
  private lateinit var accessToken: String

  val notAuthorizedEmail = "notAuthorized@liquidsoftware.io"

  @BeforeAll
  fun beforeAll() {
    accessToken = authorizeUser(testEmail, "5125550003").accessToken
    // only needed to create user
    createUser(notAuthorizedEmail, "5125550005")
  }

  @Test
  fun testNotAuthenticated() {
    // no auth token
    this.get("/api/v1/users/email/$testEmail")
      .then()
      .statusCode(403)
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
  fun testFindOtherExistingUserByEmailMatchesNotFoundContract() {
    val existing = this.get("/api/v1/users/email/$notAuthorizedEmail", accessToken)
      .then()
      .statusCode(400)
      .extract().`as`(RegisterUserErrorsDto::class.java)

    val missing = this.get("/api/v1/users/email/notMyEmail@foo.com", accessToken)
      .then()
      .statusCode(400)
      .extract().`as`(RegisterUserErrorsDto::class.java)

    assertThat(existing.errors).isEqualTo(listOf("User not found with email $notAuthorizedEmail"))
    assertThat(missing.errors).isEqualTo(listOf("User not found with email notMyEmail@foo.com"))
  }

  @Test
  fun testFindNotMyUserByEmail() {
    this.get("/api/v1/users/email/notMyEmail@foo.com", accessToken)
      .then()
      .statusCode(400)
  }

}
