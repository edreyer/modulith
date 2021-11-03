package ventures.dvx.base.user.application.port.`in`

import arrow.core.Either

// Inputs
@JvmInline
value class FindUserCommand(val username: String)

// Outputs

data class FindUserEvent(val userDto: UserDto)

// Error

@JvmInline
value class UserNotFoundError(val username: String)

// Use Case

interface FindUserUseCase {
  suspend operator fun invoke(cmd: FindUserCommand): Either<UserNotFoundError, FindUserEvent>
}
