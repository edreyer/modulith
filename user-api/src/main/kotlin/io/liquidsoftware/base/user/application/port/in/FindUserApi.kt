package io.liquidsoftware.base.user.application.port.`in`

import arrow.core.Either
import io.liquidsoftware.common.application.error.ApplicationError
import io.liquidsoftware.common.application.error.NotFoundApplicationError
import io.liquidsoftware.common.security.UserDetailsWithId
import io.liquidsoftware.common.usecase.AppEvent
import io.liquidsoftware.common.usecase.Query

// Inputs
data class FindUserByIdQuery(val userId: String) : Query
data class FindUserByEmailQuery(val email: String) : Query
data class FindUserByMsisdnQuery(val msisdn: String) : Query
data class SystemFindUserByEmailQuery(val email: String) : Query

// Events
data class UserFoundEvent(val userDto: UserDto) : AppEvent()
data class SystemUserFoundEvent(val userDetailsDto: UserDetailsWithId) : AppEvent()

// Error
data class UserNotFoundError(
  override val message: String,
) : NotFoundApplicationError {
  override val code: String = "USER_NOT_FOUND"
  override val metadata: Map<String, String> = emptyMap()
}

interface FindUserApi {
  suspend fun findUserById(query: FindUserByIdQuery): Either<ApplicationError, UserFoundEvent>
  suspend fun findUserByEmail(query: FindUserByEmailQuery): Either<ApplicationError, UserFoundEvent>
  suspend fun findUserByMsisdn(query: FindUserByMsisdnQuery): Either<ApplicationError, UserFoundEvent>
}
