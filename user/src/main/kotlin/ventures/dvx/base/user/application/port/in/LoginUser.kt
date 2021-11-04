package ventures.dvx.base.user.application.port.`in`

import arrow.core.Either

// Inputs
@JvmInline
value class FindUserByEmailCommand(val email: String)
@JvmInline
value class FindUserByMsisdnCommand(val msisdn: String)
// Outputs

data class FindUserEvent(val userDto: UserDto)

// Error

@JvmInline
value class UserNotFoundError(val lookupKey: String)

// Use Case

interface FindUserUseCase {
  suspend operator fun invoke(cmd: FindUserByEmailCommand): Either<UserNotFoundError, FindUserEvent>
}
