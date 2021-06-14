package ventures.dvx.base.user.web

import org.axonframework.commandhandling.gateway.CommandGateway
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono
import ventures.dvx.base.user.api.RegisterUserCommand
import ventures.dvx.base.user.api.User
import ventures.dvx.base.user.api.ValidateEndUserTokenCommand
import ventures.dvx.base.user.command.EndUserId
import ventures.dvx.common.logging.LoggerDelegate
import ventures.dvx.common.mapping.DataClassMapper
import ventures.dvx.common.security.JwtTokenProvider
import ventures.dvx.common.validation.Msisdn
import java.util.*
import javax.validation.Valid
import javax.validation.constraints.Email
import javax.validation.constraints.NotEmpty

// input DTO

data class RegisterEndUserInputDto(
  @NotEmpty @Msisdn val msisdn: String,
  @NotEmpty @Email val email: String,
  @NotEmpty val firstName: String,
  @NotEmpty val lastName: String
)

data class ValidateTokenInputDto(
  @NotEmpty val userId: String,
  @NotEmpty val msisdn: String,
  @NotEmpty val token: String
)

sealed class RegisterUserOutputDto
data class RegisteredUserDto(val id: UUID) : RegisterUserOutputDto()
data class RegistrationErrorDto(val err: String) : RegisterUserOutputDto()

sealed class UserLoginOutputDto
data class SuccessfulLoginDto(val accessToken: String) : UserLoginOutputDto()
data class LoginErrorDto(val err: String) : UserLoginOutputDto()

@RestController
class RegisterUserController(
  private val commandGateway: CommandGateway,
  private val tokenProvider: JwtTokenProvider
) {

  val log by LoggerDelegate()

  @PostMapping(path = ["/user/register"])
  fun register(@Valid @RequestBody input: RegisterEndUserInputDto): Mono<ResponseEntity<RegisterUserOutputDto>> =
    commandGateway.send<EndUserId>(input.toCommand())
      .thenApply { RegisteredUserDto(it.id) }
      .let { Mono.fromFuture(it) }
      .map { ResponseEntity.ok(RegisteredUserDto(it.id) as RegisterUserOutputDto) }
      .onErrorResume {
        Mono.just(ResponseEntity
          .status(HttpStatus.UNAUTHORIZED)
          .body(RegistrationErrorDto(it.message ?: "RegistrationError") as RegisterUserOutputDto))
      }


  @PostMapping(path = ["/user/confirmToken"])
  fun confirmToken(@Valid @RequestBody input: ValidateTokenInputDto)
  : Mono<ResponseEntity<UserLoginOutputDto>>
  {
    return commandGateway.send<User>(input.toCommand())
      .let { Mono.fromFuture(it) }
      .map { tokenProvider.createToken(input.userId, it.roles.map { role -> SimpleGrantedAuthority(role) }) }
      .map {
        val headers = HttpHeaders()
        headers[HttpHeaders.AUTHORIZATION] = "Bearer $it"
        val tokenBody =
          ResponseEntity.ok(SuccessfulLoginDto(it) as UserLoginOutputDto)
        tokenBody
      }
      .onErrorResume {
        Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(LoginErrorDto("Unauthorized") as UserLoginOutputDto))
      }
  }

}

fun RegisterEndUserInputDto.toCommand(): RegisterUserCommand =
  DataClassMapper<RegisterEndUserInputDto, RegisterUserCommand>()
    .targetParameterSupplier(RegisterUserCommand::userId) { EndUserId() } (this)

fun ValidateTokenInputDto.toCommand(): ValidateEndUserTokenCommand =
  ValidateEndUserTokenCommand(
    id = EndUserId(UUID.fromString(this.userId)),
    msisdn = this.msisdn,
    token = this.token
  )
