package ventures.dvx.base.user.application.usecase

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import ventures.dvx.base.user.application.port.`in`.FindUserCommand
import ventures.dvx.base.user.application.port.`in`.FindUserEvent
import ventures.dvx.base.user.application.port.`in`.FindUserUseCase
import ventures.dvx.base.user.application.port.`in`.UserNotFoundError
import ventures.dvx.base.user.application.port.out.FindUserPort
import ventures.dvx.base.user.application.usecase.mapper.toUserDto

class FindUserUseCaseImpl(
  private val findUserPort: FindUserPort
) : FindUserUseCase {

  override suspend fun invoke(cmd: FindUserCommand):
    Either<UserNotFoundError, FindUserEvent> = findUserPort
      .findUserByUsername(cmd.username)?.let {
        FindUserEvent(it.toUserDto()).right()
      } ?: UserNotFoundError(cmd.username).left()
}
