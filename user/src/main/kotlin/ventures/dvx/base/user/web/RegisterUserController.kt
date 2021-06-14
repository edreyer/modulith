package ventures.dvx.base.user.web

import org.axonframework.commandhandling.gateway.CommandGateway
import org.springframework.http.HttpHeaders
import org.springframework.security.authentication.ReactiveAuthenticationManager
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
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

sealed class UserLoginOutputDto
data class SuccessfulLogin(val accessToken: String) : UserLoginOutputDto()
data class LoginError(val err: String) : UserLoginOutputDto()

@RestController
class RegisterUserController(
  private val commandGateway: CommandGateway,
  private val tokenProvider: JwtTokenProvider,
  private val authenticationManager: ReactiveAuthenticationManager
) {

  val log by LoggerDelegate()

  @PostMapping(path = ["/user/register"])
  suspend fun register(@Valid @RequestBody input: RegisterEndUserInputDto): RegisteredUserDto =
    commandGateway.send<EndUserId>(input.toCommand())
      .thenApply { RegisteredUserDto(it.id) }
      .get() // TODO Can't seem to get Mono/Future working as return type


  @PostMapping(path = ["/user/confirmToken"])
  suspend fun confirmToken(@Valid @RequestBody input: ValidateTokenInputDto)
  : UserLoginOutputDto
  {
    // TODO: Load real user roles with user data
    val authorities = listOf("ROLE_USER")
      .map { SimpleGrantedAuthority(it) }

    return commandGateway.send<User>(input.toCommand())
      .get()
      .let { tokenProvider.createToken(input.userId, authorities) }
      .let {
        val headers = HttpHeaders()
        headers[HttpHeaders.AUTHORIZATION] = "Bearer $it"
        val tokenBody = SuccessfulLogin(it) as UserLoginOutputDto
        tokenBody
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
