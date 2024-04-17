package io.liquidsoftware.base.user.adapter.`in`.web

import io.liquidsoftware.common.logging.LoggerDelegate
import io.liquidsoftware.common.security.JwtTokenService
import jakarta.validation.Valid
import jakarta.validation.constraints.NotEmpty
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

data class UserLoginInputDto(
  @NotEmpty val username: String,
  @NotEmpty val password: String
)

sealed class UserLoginOutputDto
data class SuccessfulLogin(val accessToken: String) : UserLoginOutputDto()
data class LoginError(val err: String) : UserLoginOutputDto()

@RestController
class AuthController(
  private val tokenService: JwtTokenService,
  private val authenticationManager: AuthenticationManager
) {

  val logger by LoggerDelegate()

  @PostMapping("/auth/login")
  suspend fun login(@Valid @RequestBody loginDto: UserLoginInputDto):
    ResponseEntity<UserLoginOutputDto> {
    val auth =  authenticationManager
      .authenticate(UsernamePasswordAuthenticationToken(loginDto.username, loginDto.password))

    return if (auth.isAuthenticated) {
      auth
        .let { tokenService.generateToken(loginDto.username, auth.authorities) }
        .let {
          logger.debug("Authenticated User: ${loginDto.username}")
          val headers = HttpHeaders()
          headers[HttpHeaders.AUTHORIZATION] = "Bearer $it"
          val tokenBody = SuccessfulLogin(it)
          ResponseEntity(tokenBody, headers, HttpStatus.OK)
        }
    } else {
      logger.debug("Failed to login with: {}", loginDto)
      ResponseEntity.badRequest().body(
        LoginError("Unknown user: ${loginDto.username}")
      )
    }
  }

}
