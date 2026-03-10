package io.liquidsoftware.base.user.application.port.`in`

import arrow.core.Either
import io.liquidsoftware.common.application.error.ApplicationError

interface SystemFindUserByEmailApi {
  suspend fun findSystemUserByEmail(query: SystemFindUserByEmailQuery): Either<ApplicationError, SystemUserFoundEvent>
}
