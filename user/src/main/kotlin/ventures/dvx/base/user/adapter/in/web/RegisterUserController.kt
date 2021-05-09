package ventures.dvx.base.user.adapter.`in`.web

import arrow.core.Nel
import arrow.core.nel
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import ventures.dvx.base.user.application.port.`in`.RegisterUserCommand
import ventures.dvx.base.user.application.port.`in`.RegisterUserError.UserExistsError
import ventures.dvx.base.user.application.port.`in`.RegisterUserError.UserValidationErrors
import ventures.dvx.base.user.application.port.`in`.RegisterUserEvent
import ventures.dvx.base.user.application.port.`in`.RegisterUserEvent.ValidUserRegistration
import ventures.dvx.base.user.application.port.`in`.RegisterUserUseCase
import ventures.dvx.common.mapping.DataClassMapper
import javax.validation.Valid
import javax.validation.constraints.Email
import javax.validation.constraints.NotEmpty

// input DTO

data class RegisterUserInputDto(
  @NotEmpty val username: String,
  @NotEmpty @Email val email: String,
  @NotEmpty val password: String
)

sealed class RegisterUserOutputDto
data class RegisteredUserDto(val username: String, val email: String) : RegisterUserOutputDto()
data class RegisterUserErrorsDto(val errors: Nel<String>) : RegisterUserOutputDto()

@RestController
class RegisterUserController(
  private val registerUserUseCase: RegisterUserUseCase
) {

  @PostMapping("/auth/register")
  suspend fun register(@Valid @RequestBody registerUser: RegisterUserInputDto)
  : ResponseEntity<RegisterUserOutputDto> =
    registerUserUseCase(registerUser.toCommand())
      .fold({ when (it) {
        is UserExistsError -> ResponseEntity.badRequest().body(it.toOutputDto())
        is UserValidationErrors -> ResponseEntity.badRequest().body(it.toOutputDto())
      }
      },{
        ResponseEntity.ok(it.toOutputDto())
      })
}

fun RegisterUserInputDto.toCommand(): RegisterUserCommand =
  DataClassMapper<RegisterUserInputDto, RegisterUserCommand>()(this)

fun UserExistsError.toOutputDto(): RegisterUserOutputDto =
  DataClassMapper<UserExistsError, RegisterUserErrorsDto>()
    .targetParameterSupplier("errors") { this.error.nel() } (this)

fun UserValidationErrors.toOutputDto(): RegisterUserOutputDto =
  DataClassMapper<UserValidationErrors, RegisterUserErrorsDto>()
    .targetParameterSupplier("errors") { this.errors.map { it.error } } (this)

fun Nel<RegisterUserEvent>.toOutputDto(): RegisterUserOutputDto = this
  .filterIsInstance<ValidUserRegistration>()
  .map { DataClassMapper<ValidUserRegistration, RegisteredUserDto>()(it) }
  .first()

