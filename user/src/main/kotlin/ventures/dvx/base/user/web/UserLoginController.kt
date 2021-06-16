package ventures.dvx.base.user.web

import org.axonframework.commandhandling.gateway.CommandGateway
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.authentication.ReactiveAuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono
import ventures.dvx.base.user.api.EndUserId
import ventures.dvx.base.user.api.LoginEndUserCommand
import ventures.dvx.base.user.api.User
import ventures.dvx.base.user.api.ValidateEndUserTokenCommand
import ventures.dvx.base.user.command.EndUser
import ventures.dvx.common.axon.command.persistence.IndexRepository
import ventures.dvx.common.security.JwtTokenProvider
import ventures.dvx.common.validation.Msisdn
import java.util.*
import javax.validation.Valid
import javax.validation.constraints.Email
import javax.validation.constraints.NotEmpty

data class EmailLoginDto(
  @NotEmpty @Email val email: String,
  @NotEmpty val password: String,
)
data class MsisdnLoginDto(
  @NotEmpty @Msisdn val msisdn: String
)

@RestController
class UserLoginController(
  private val commandGateway: CommandGateway,
  private val tokenProvider: JwtTokenProvider,
  private val authenticationManager: ReactiveAuthenticationManager,
  private val indexRepository: IndexRepository
) {

  @PostMapping(path = ["/user/loginByEmail"])
  fun loginWithEmail(@Valid @RequestBody input: EmailLoginDto)
    : Mono<ResponseEntity<EmailLoginOutputDto>>
  {
    return authenticationManager.authenticate(
        UsernamePasswordAuthenticationToken(input.email, input.password)
      )
      .map { tokenProvider.createToken(it) }
      .map {
        val headers = HttpHeaders()
        headers[HttpHeaders.AUTHORIZATION] = "Bearer $it"
        ResponseEntity.ok(SuccessfulEmailLoginDto(it) as EmailLoginOutputDto)
      }
      .onErrorResume {
        Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED)
          .body(EmailLoginErrorDto("Unauthorized") as EmailLoginOutputDto))
      }
  }

  @PostMapping(path = ["/user/loginByMsisdn"])
  fun loginWithMsisdn(@Valid @RequestBody input: MsisdnLoginDto)
    : Mono<ResponseEntity<MsisdnLoginStartedOutputDto>> {
    val command = input.let {
      indexRepository.findEntityByAggregateNameAndKey(EndUser.aggregateName(), it.msisdn)
    }
      ?.let { LoginEndUserCommand(EndUserId(it.aggregateId), input.msisdn) }
      ?: throw IllegalArgumentException("Unknown User")

    return commandGateway.send<EndUserId>(command)
      .let { Mono.fromFuture(it) }
      .map { ResponseEntity.ok(SuccessfulMsisdnLoginDto() as MsisdnLoginStartedOutputDto) }
      .onErrorResume {
        Mono.just(ResponseEntity
          .status(HttpStatus.UNAUTHORIZED)
          .body(MsisdnLoginErrorDto(it.message ?: "Login Error") as MsisdnLoginStartedOutputDto))
      }
  }

  @PostMapping(path = ["/user/confirmToken"])
  fun confirmToken(@Valid @RequestBody input: ValidateTokenInputDto)
    : Mono<ResponseEntity<EmailLoginOutputDto>>
  {
    return commandGateway.send<User>(input.toCommand())
      .let { Mono.fromFuture(it) }
      .map { tokenProvider.createToken(input.userId, it.roles.map { role -> SimpleGrantedAuthority(role) }) }
      .map {
        val headers = HttpHeaders()
        headers[HttpHeaders.AUTHORIZATION] = "Bearer $it"
        val tokenBody =
          ResponseEntity.ok(SuccessfulEmailLoginDto(it) as EmailLoginOutputDto)
        tokenBody
      }
      .onErrorResume {
        Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED)
          .body(EmailLoginErrorDto("Unauthorized") as EmailLoginOutputDto))
      }

  }

  fun ValidateTokenInputDto.toCommand(): ValidateEndUserTokenCommand =
    ValidateEndUserTokenCommand(
      userId = EndUserId(UUID.fromString(this.userId)),
      msisdn = this.msisdn,
      token = this.token
    )
}
