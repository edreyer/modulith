package ventures.dvx.auth

import kotlinx.coroutines.reactor.awaitSingle
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.authentication.ReactiveAuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono
import ventures.dvx.common.logging.LoggerDelegate
import ventures.dvx.security.JwtTokenProvider
import javax.validation.Valid
import javax.validation.constraints.NotEmpty

data class UserLoginInputDto(
  @NotEmpty val username: String,
  @NotEmpty val password: String
)

sealed class UserLoginOutputDto
data class SuccessfulLogin(val accessToken: String) : UserLoginOutputDto()
data class LoginError(val err: String) : UserLoginOutputDto()

@RestController
class AuthController(
  private val tokenProvider: JwtTokenProvider,
  private val authenticationManager: ReactiveAuthenticationManager
) {

  val logger by LoggerDelegate()

  @PostMapping("/auth/login")
  suspend fun login(@Valid @RequestBody loginDto: UserLoginInputDto):
    ResponseEntity<UserLoginOutputDto> =
    authenticationManager
      .authenticate(UsernamePasswordAuthenticationToken(loginDto.username, loginDto.password))
      .map { tokenProvider.createToken(it) }
      .map<ResponseEntity<UserLoginOutputDto>> {
        val headers = HttpHeaders()
        headers[HttpHeaders.AUTHORIZATION] = "Bearer $it"
        val tokenBody = SuccessfulLogin(it)
        ResponseEntity(tokenBody, headers, HttpStatus.OK)
      }.onErrorResume {
        logger.debug("Failed to login with: $loginDto")
        Mono.just(ResponseEntity.badRequest().body(
          LoginError("Unknown user: ${loginDto.username}")
        ))
      }.awaitSingle()

}
