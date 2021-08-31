package ventures.dvx.base.user.web

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import ventures.dvx.base.user.usecase.RegisterAdminUserUseCase
import ventures.dvx.base.user.usecase.RegisterEndUserUseCase
import ventures.dvx.common.logging.LoggerDelegate
import ventures.dvx.common.validation.Msisdn
import java.util.*
import javax.validation.Valid
import javax.validation.constraints.Email
import javax.validation.constraints.NotEmpty

// input DTOs

data class RegisterEndUserInputDto(
  @NotEmpty @Msisdn val msisdn: String,
  @NotEmpty @Email val email: String,
  @NotEmpty val firstName: String,
  @NotEmpty val lastName: String
)

// output DTOs

sealed class RegisterUserOutputDto : OutputDto
data class RegisteredUserDto(val id: UUID) : RegisterUserOutputDto()


@RestController
class RegisterUserController(
  private val registerAdminUserUseCase: RegisterAdminUserUseCase,
  private val registerEndUserUseCase: RegisterEndUserUseCase,
) : BaseUserController() {

  val log by LoggerDelegate()

  @PostMapping(path = ["/admin/register"])
  suspend fun registerAdmin(): ResponseEntity<OutputDto> = registerAdminUserUseCase.run(Unit)
    .mapToResponse { ResponseEntity.ok(it) }

  @PostMapping(path = ["/user/register"])
  suspend fun register(@Valid @RequestBody input: RegisterEndUserInputDto)
    : ResponseEntity<OutputDto> = registerEndUserUseCase.run(input)
    .mapToResponse { ResponseEntity.ok(it) }

}
