package ventures.dvx.base.user.application.port.`in`

import arrow.core.Either

// Inputs
inline class FindUserCommand(val username: String)

// Outputs

data class FindUserEvent(val userDto: UserDto)

// Error

inline class UserNotFoundError(val username: String)

// Use Case

interface FindUserUseCase {
  suspend operator fun invoke(cmd: FindUserCommand): Either<UserNotFoundError, FindUserEvent>
}
