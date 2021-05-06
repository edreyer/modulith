package ventures.dvx.base.user.application.port.`in`

import arrow.core.Either
import arrow.core.Nel

// Input

data class RegisterUserCommand(
  val username: String,
  val email: String,
  val password: String
)

// Output

sealed class RegisterUserEvent {
  data class ValidUserRegistration(
    val username: String,
    val email: String
  ) : RegisterUserEvent()
}


sealed class RegisterUserError {
  data class UserExistsError(val error: String) : RegisterUserError()
}

// Use Case

interface RegisterUserUseCase {
  suspend operator fun invoke(cmd: RegisterUserCommand): Either<RegisterUserError, Nel<RegisterUserEvent>>
}

