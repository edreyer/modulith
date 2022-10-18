package io.liquidsoftware.base.user.adapter.`in`.web

import io.liquidsoftware.base.user.application.port.`in`.RegisterUserCommand
import io.liquidsoftware.base.user.application.port.`in`.RoleDto
import io.liquidsoftware.base.user.application.port.`in`.UserDto
import io.liquidsoftware.base.user.application.port.`in`.UserExistsError
import io.liquidsoftware.base.user.application.port.`in`.UserRegisteredEvent
import io.liquidsoftware.common.types.ValidationException
import io.liquidsoftware.common.validation.Msisdn
import io.liquidsoftware.common.workflow.WorkflowDispatcher
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import javax.validation.Valid
import javax.validation.constraints.Email
import javax.validation.constraints.NotEmpty

// input DTO

data class RegisterUserInputDto(
  @NotEmpty @Msisdn val msisdn: String,
  @NotEmpty @Email val email: String,
  @NotEmpty val password: String,
  val role: RoleDto
)

sealed class RegisterUserOutputDto
data class RegisteredUserDto(val user: UserDto) : RegisterUserOutputDto()
data class RegisterUserErrorsDto(val errors: String) : RegisterUserOutputDto()

@RestController
internal class RegisterUserController {

  @PostMapping("/user/register")
  suspend fun register(@Valid @RequestBody registerUser: RegisterUserInputDto)
    : ResponseEntity<RegisterUserOutputDto> {

    val foo = WorkflowDispatcher.dispatch<UserRegisteredEvent>(registerUser.toCommand())
    return foo.fold(
      { ResponseEntity.ok(it.toOutputDto()) },
      {
        when (it) {
          is UserExistsError -> ResponseEntity.badRequest().body(it.toOutputDto())
          is ValidationException -> ResponseEntity.badRequest().body(it.toOutputDto())
          else -> ResponseEntity.status(500).body(it.toOutputDto())
        }
      },
    )
  }

  suspend fun RegisterUserInputDto.toCommand(): RegisterUserCommand =
    RegisterUserCommand(
      msisdn = this.msisdn,
      email = this.email,
      password = this.password,
      role = this.role.name
    )

  fun UserExistsError.toOutputDto(): RegisterUserOutputDto =
    RegisterUserErrorsDto(this.error)

  fun ValidationException.toOutputDto(): RegisterUserOutputDto =
    RegisterUserErrorsDto(this.errorString)

  fun Throwable.toOutputDto(): RegisterUserOutputDto =
    RegisterUserErrorsDto("Server Error: ${this.message}")

  fun UserRegisteredEvent.toOutputDto(): RegisterUserOutputDto = RegisteredUserDto(this.userDto)
}

