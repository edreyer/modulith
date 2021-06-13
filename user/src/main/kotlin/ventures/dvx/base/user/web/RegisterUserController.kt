package ventures.dvx.base.user.web

import org.axonframework.commandhandling.gateway.CommandGateway
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import ventures.dvx.base.user.api.EndUserId
import ventures.dvx.base.user.api.RegisterUserCommand
import ventures.dvx.base.user.api.ValidateEndUserTokenCommand
import ventures.dvx.common.logging.LoggerDelegate
import ventures.dvx.common.mapping.DataClassMapper
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

@RestController
class RegisterUserController(
  private val commandGateway: CommandGateway
) {

  val log by LoggerDelegate()

  @PostMapping(path = ["/user/register"])
  suspend fun register(@Valid @RequestBody input: RegisterEndUserInputDto)
  : RegisteredUserDto =
    commandGateway.send<EndUserId>(input.toCommand())
      .thenApply { RegisteredUserDto(it.id) }
      .get() // TODO Can't seem to get Mono/Future working as return type


  @PostMapping(path = ["/user/confirmToken"])
  suspend fun confirmToken(@Valid @RequestBody input: ValidateTokenInputDto)
    : ResponseEntity<Unit> =
    commandGateway.send<Unit>(input.toCommand())
      .thenApply { ResponseEntity.ok(Unit) }
      .get() // TODO Can't seem to get Mono/Future working as return type

}

fun RegisterEndUserInputDto.toCommand(): RegisterUserCommand =
  DataClassMapper<RegisterEndUserInputDto, RegisterUserCommand>()(this)

fun ValidateTokenInputDto.toCommand(): ValidateEndUserTokenCommand =
  ValidateEndUserTokenCommand(
    id = EndUserId(UUID.fromString(this.userId)),
    msisdn = this.msisdn,
    token = this.token
  )
