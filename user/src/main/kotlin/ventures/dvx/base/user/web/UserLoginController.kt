package ventures.dvx.base.user.web

import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.authentication.ReactiveAuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono
import ventures.dvx.common.security.JwtTokenProvider
import javax.validation.Valid
import javax.validation.constraints.Email
import javax.validation.constraints.NotEmpty

data class UserLoginDto(
  @NotEmpty @Email val email: String,
  @NotEmpty val password: String,
)

@RestController
class UserLoginController(
  private val tokenProvider: JwtTokenProvider,
  private val authenticationManager: ReactiveAuthenticationManager
) {

  @PostMapping(path = ["/user/login"])
  fun userLogin(@Valid @RequestBody input: UserLoginDto)
    : Mono<ResponseEntity<UserLoginOutputDto>>
  {
    return authenticationManager.authenticate(
        UsernamePasswordAuthenticationToken(input.email, input.password)
      )
      .map { tokenProvider.createToken(it) }
      .map {
        val headers = HttpHeaders()
        headers[HttpHeaders.AUTHORIZATION] = "Bearer $it"
        ResponseEntity.ok(SuccessfulLoginDto(it) as UserLoginOutputDto)
      }
      .onErrorResume {
        Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED)
          .body(LoginErrorDto("Unauthorized") as UserLoginOutputDto))
      }
  }

}
