package ventures.dvx.base.user.web

import org.springframework.http.HttpHeaders
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import ventures.dvx.base.user.usecase.ConfirmTokenUseCase
import ventures.dvx.base.user.usecase.LoginByEmailUseCase
import ventures.dvx.base.user.usecase.LoginByMsisdnUseCase
import ventures.dvx.common.validation.Msisdn
import javax.validation.Valid
import javax.validation.constraints.Email
import javax.validation.constraints.NotEmpty

// DTOs

data class EmailLoginDto(
  @NotEmpty @Email val email: String,
  @NotEmpty val password: String,
)

data class MsisdnLoginDto(
  @NotEmpty @Msisdn val msisdn: String
)

data class ValidateTokenInputDto(
  @NotEmpty val userId: String,
  @NotEmpty @Msisdn val msisdn: String,
  @NotEmpty val token: String
)

@RestController
class UserLoginController(
  private val loginByEmailUseCase: LoginByEmailUseCase,
  private val loginByMsisdnUseCase: LoginByMsisdnUseCase,
  private val confirmTokenUseCase: ConfirmTokenUseCase
): BaseUserController() {

  @PostMapping(path = ["/user/loginByEmail"])
  suspend fun loginWithEmail(@Valid @RequestBody input: EmailLoginDto)
    : ResponseEntity<OutputDto> = loginByEmailUseCase.run(input)
    .mapToResponse {
      val headers = HttpHeaders()
      headers[HttpHeaders.AUTHORIZATION] = "Bearer $it"
      ResponseEntity.ok(it)
    }

  @PostMapping(path = ["/user/loginByMsisdn"])
  suspend fun loginWithMsisdn(@Valid @RequestBody input: MsisdnLoginDto)
    : ResponseEntity<OutputDto> = loginByMsisdnUseCase.run(input)
    .mapToResponse { ResponseEntity.ok(SuccessfulMsisdnLoginDto(it.id)) }

  @PostMapping(path = ["/user/confirmToken"])
  suspend fun confirmToken(@Valid @RequestBody input: ValidateTokenInputDto)
    : ResponseEntity<OutputDto> = confirmTokenUseCase.run(input)
        .mapToResponse {
          val headers = HttpHeaders()
          headers[HttpHeaders.AUTHORIZATION] = "Bearer $it"
          ResponseEntity.ok(it)
        }

}
