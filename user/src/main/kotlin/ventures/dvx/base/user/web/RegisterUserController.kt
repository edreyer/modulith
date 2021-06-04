package ventures.dvx.base.user.web

import org.axonframework.commandhandling.gateway.CommandGateway
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import ventures.dvx.base.user.api.RegisterUserCommand
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

sealed class RegisterUserOutputDto
data class RegisteredUserDto(val id: UUID) : RegisterUserOutputDto()

@RestController
class RegisterUserController(
  private val commandGateway: CommandGateway
) {

  @PostMapping(path = ["/user/register"])
  suspend fun register(@Valid @RequestBody input: RegisterEndUserInputDto)
  : ResponseEntity<RegisteredUserDto> {
    return commandGateway.send<UUID>(input.toCommand())
      .thenApply { ResponseEntity.ok(RegisteredUserDto(it)) }
      .get()
  }

}

fun RegisterEndUserInputDto.toCommand(): RegisterUserCommand =
  DataClassMapper<RegisterEndUserInputDto, RegisterUserCommand>()(this)

//fun UserExistsError.toOutputDto(): RegisterUserOutputDto =
//  DataClassMapper<UserExistsError, RegisterUserErrorsDto>()
//    .targetParameterSupplier("errors") { listOf(this.error) } (this)
//
//fun UserValidationErrors.toOutputDto(): RegisterUserOutputDto =
//  DataClassMapper<UserValidationErrors, RegisterUserErrorsDto>()
//    .targetParameterSupplier("errors") { this.errors.map { it.error }.toList() } (this)
//
//fun Nel<RegisterUserEvent>.toOutputDto(): RegisterUserOutputDto = this
//  .filterIsInstance<ValidUserRegistration>()
//  .map { DataClassMapper<ValidUserRegistration, RegisteredUserDto>()(it) }
//  .first()
//
