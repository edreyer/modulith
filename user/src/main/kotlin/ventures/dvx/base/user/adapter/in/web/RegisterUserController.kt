package ventures.dvx.base.user.adapter.`in`.web

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import ventures.dvx.base.user.application.port.`in`.RegisterUserCommand
import ventures.dvx.base.user.application.port.`in`.RegisterUserError.UserExistsError
import ventures.dvx.base.user.application.port.`in`.RegisterUserEvent
import ventures.dvx.base.user.application.port.`in`.RegisterUserEvent.ValidUserRegistration
import ventures.dvx.base.user.application.port.`in`.RegisterUserWorkflow
import ventures.dvx.common.types.ValidationException
import ventures.dvx.common.types.toErrStrings
import ventures.dvx.common.validation.Msisdn
import ventures.dvx.common.workflow.RequestDispatcher
import javax.validation.Valid
import javax.validation.constraints.Email
import javax.validation.constraints.NotEmpty

// input DTO

data class RegisterUserInputDto(
  @NotEmpty @Msisdn val msisdn: String,
  @NotEmpty @Email val email: String,
  @NotEmpty val password: String
)

sealed class RegisterUserOutputDto
data class RegisteredUserDto(val msisdn: String, val email: String) : RegisterUserOutputDto()
data class RegisterUserErrorsDto(val errors: List<String>) : RegisterUserOutputDto()

@RestController
class RegisterUserController(
  registerUserWorkflow: RegisterUserWorkflow
) {

  init {
    RequestDispatcher.registerCommandHandler(registerUserWorkflow)
  }

  @PostMapping("/user/register")
  suspend fun register(@Valid @RequestBody registerUser: RegisterUserInputDto)
    : ResponseEntity<RegisterUserOutputDto> =
    RequestDispatcher.dispatch<RegisterUserEvent>(registerUser.toCommand())
      .fold(
        { ResponseEntity.ok(it.toOutputDto()) },
        {
          when (it) {
            is UserExistsError -> ResponseEntity.badRequest().body(it.toOutputDto())
            is ValidationException -> ResponseEntity.badRequest().body(it.toOutputDto())
            else -> ResponseEntity.badRequest().body(it.toOutputDto())
          }
        }
      )

  fun RegisterUserInputDto.toCommand(): RegisterUserCommand =
    RegisterUserCommand(
      msisdn = this.msisdn,
      email = this.email,
      password = this.password
    )

  fun UserExistsError.toOutputDto(): RegisterUserOutputDto =
    RegisterUserErrorsDto(listOf(this.error))

  fun ValidationException.toOutputDto(): RegisterUserOutputDto =
    RegisterUserErrorsDto(this.errors.toErrStrings())

  fun Throwable.toOutputDto(): RegisterUserOutputDto =
    RegisterUserErrorsDto(listOf("Server Error: ${this.message}"))

  fun RegisterUserEvent.toOutputDto(): RegisterUserOutputDto = when (this) {
    is ValidUserRegistration -> RegisteredUserDto(
      msisdn = this.msisdn,
      email = this.email
    )
  }
}

