package ventures.dvx.auth

import ventures.dvx.test.BaseWebTest

class AuthenticationTest : BaseWebTest() {

//  private lateinit var inputDto: UserLoginInputDto
//
//  val testData = RegisterUserInputDto(
//    username = "bob.loblaw",
//    email = "bob@loblaw.com",
//    password = "password"
//  )
//
//  @BeforeAll
//  fun initUser() {
//    inputDto = UserLoginInputDto(
//      username = testData.username,
//      password = testData.password
//    )
//  }
//
//  @Test
//  fun `successful login`() {
//    registerUser(testData)
//    val actual = post("/auth/login", inputDto)
//      .then()
//      .statusCode(200)
//      .extract().`as`(SuccessfulLogin::class.java)
//    assertThat(actual.accessToken).isNotBlank
//  }
//
//  @Test
//  fun `unsuccessful login`() {
//    val actual = post(
//      "/auth/login",
//      UserLoginInputDto("foo", "bar")
//    )
//      .then()
//      .statusCode(400)
//      .extract().`as`(LoginError::class.java)
//    assertThat(actual.err).isNotBlank
//  }

}
