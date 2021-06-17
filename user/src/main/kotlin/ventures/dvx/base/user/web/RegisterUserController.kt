package ventures.dvx.base.user.web

import org.axonframework.extensions.reactor.commandhandling.gateway.ReactorCommandGateway
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono
import ventures.dvx.base.user.api.EndUserId
import ventures.dvx.base.user.api.RegisterEndUserCommand
import ventures.dvx.common.logging.LoggerDelegate
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
  @NotEmpty @Msisdn val msisdn: String,
  @NotEmpty val token: String
)

sealed class RegisterUserOutputDto
data class RegisteredUserDto(val id: UUID) : RegisterUserOutputDto()
data class RegistrationErrorDto(val err: String) : RegisterUserOutputDto()


@RestController
class RegisterUserController(
  private val commandGateway: ReactorCommandGateway
) {

  val log by LoggerDelegate()

  @PostMapping(path = ["/user/register"])
  fun register(@Valid @RequestBody input: RegisterEndUserInputDto): Mono<ResponseEntity<RegisterUserOutputDto>> =
    commandGateway.send<EndUserId>(input.toCommand())
      .map { RegisteredUserDto(it.id) }
      .map { ResponseEntity.ok(RegisteredUserDto(it.id) as RegisterUserOutputDto) }
      .onErrorResume {
        Mono.just(ResponseEntity
          .status(HttpStatus.UNAUTHORIZED)
          .body(RegistrationErrorDto(it.message ?: "RegistrationError") as RegisterUserOutputDto))
      }
}

fun RegisterEndUserInputDto.toCommand(): RegisterEndUserCommand =
  RegisterEndUserCommand(
    userId = EndUserId(),
    msisdn = this.msisdn,
    email = this.email,
    firstName = this.firstName,
    lastName = this.lastName
  )


