package ventures.dvx.base.user.web

import arrow.core.ValidatedNel
import org.axonframework.extensions.reactor.commandhandling.gateway.ReactorCommandGateway
import org.springframework.http.HttpHeaders
import org.springframework.http.ResponseEntity
import org.springframework.security.authentication.ReactiveAuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono
import ventures.dvx.base.user.api.EndUserId
import ventures.dvx.base.user.api.InvalidInput
import ventures.dvx.base.user.api.LoginEndUserCommand
import ventures.dvx.base.user.api.User
import ventures.dvx.base.user.api.UserNotFoundError
import ventures.dvx.base.user.api.ValidateEndUserTokenCommand
import ventures.dvx.base.user.command.EndUser
import ventures.dvx.common.axon.command.persistence.IndexJpaEntity
import ventures.dvx.common.axon.command.persistence.IndexRepository
import ventures.dvx.common.security.JwtTokenProvider
import ventures.dvx.common.types.ValidationError
import ventures.dvx.common.types.toErrStrings
import ventures.dvx.common.validation.Msisdn
import ventures.dxv.base.user.error.UserException
import java.util.*
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

@RestController
class UserLoginController(
  private val commandGateway: ReactorCommandGateway,
  private val tokenProvider: JwtTokenProvider,
  private val authenticationManager: ReactiveAuthenticationManager,
  private val indexRepository: IndexRepository
): BaseUserController() {

  @PostMapping(path = ["/user/loginByEmail"])
  fun loginWithEmail(@Valid @RequestBody input: EmailLoginDto)
    : Mono<ResponseEntity<OutputDto>>
  {
    return authenticationManager.authenticate(
        UsernamePasswordAuthenticationToken(input.email, input.password)
      )
      .map { tokenProvider.createToken(it) }
      .map {
        val headers = HttpHeaders()
        headers[HttpHeaders.AUTHORIZATION] = "Bearer $it"
        ResponseEntity.ok(SuccessfulEmailLoginDto(it) as OutputDto)
      }
      .mapErrorToResponseEntity()
  }

  @PostMapping(path = ["/user/loginByMsisdn"])
  fun loginWithMsisdn(@Valid @RequestBody input: MsisdnLoginDto)
    : Mono<ResponseEntity<OutputDto>> {

    val user: Mono<IndexJpaEntity> = indexRepository.findEntityByAggregateNameAndKey(EndUser.aggregateName(), input.msisdn)
      ?.let { Mono.just(it) } ?: Mono.error(UserException(UserNotFoundError(input.msisdn)))

    return user
      .map { LoginEndUserCommand.of(EndUserId(it.id), it.key) }
      .flatMap { it.fold(
        { errors -> Mono.error(UserException(InvalidInput(errors.toErrStrings())))},
        { cmd -> Mono.just(cmd) }
      )}
      .flatMap { commandGateway.send<EndUserId>(it) }
      .map { ResponseEntity.ok(SuccessfulMsisdnLoginDto(it.id) as OutputDto) }
      .mapErrorToResponseEntity()
  }

  @PostMapping(path = ["/user/confirmToken"])
  fun confirmToken(@Valid @RequestBody input: ValidateTokenInputDto)
    : Mono<ResponseEntity<OutputDto>> =
    input.toCommand().fold(
      { errors -> Mono.error(UserException(InvalidInput(errors.toErrStrings())))},
      { cmd -> Mono.just(cmd) }
    )
      .flatMap { commandGateway.send<User>(it) }
      .map { tokenProvider.createToken(input.userId, it.roles.map { role -> SimpleGrantedAuthority(role) }) }
      .map {
        val headers = HttpHeaders()
        headers[HttpHeaders.AUTHORIZATION] = "Bearer $it"
        val tokenBody =
          ResponseEntity.ok(SuccessfulEmailLoginDto(it) as OutputDto)
        tokenBody
      }
      .mapErrorToResponseEntity()

  fun ValidateTokenInputDto.toCommand(): ValidatedNel<ValidationError, ValidateEndUserTokenCommand> =
    ValidateEndUserTokenCommand.of(
      userId = EndUserId(UUID.fromString(this.userId)),
      msisdn = this.msisdn,
      token = this.token
    )
}
