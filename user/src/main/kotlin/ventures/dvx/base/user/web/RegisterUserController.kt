package ventures.dvx.base.user.web

import org.axonframework.commandhandling.gateway.CommandGateway
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import ventures.dvx.base.user.api.EndUserId
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
data class RegisteredUserDto(val id: EndUserId) : RegisterUserOutputDto()

@RestController
class RegisterUserController(
  private val commandGateway: CommandGateway
) {

  @PostMapping(path = ["/user/register"])
  suspend fun register(@Valid @RequestBody input: RegisterEndUserInputDto)
  : ResponseEntity<RegisteredUserDto> {
    return commandGateway.send<EndUserId>(input.toCommand())
      .thenApply { ResponseEntity.ok(RegisteredUserDto(it)) }
      .get()
  }

}

fun RegisterEndUserInputDto.toCommand(): RegisterUserCommand =
  DataClassMapper<RegisterEndUserInputDto, RegisterUserCommand>()(this)
