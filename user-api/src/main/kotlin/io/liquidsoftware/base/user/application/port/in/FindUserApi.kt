package io.liquidsoftware.base.user.application.port.`in`

import arrow.core.Either
import io.liquidsoftware.common.security.UserDetailsWithId
import io.liquidsoftware.common.workflow.Event
import io.liquidsoftware.common.workflow.Query
import io.liquidsoftware.common.workflow.WorkflowError

// Inputs
data class FindUserByIdQuery(val userId: String) : Query
data class FindUserByEmailQuery(val email: String) : Query
data class FindUserByMsisdnQuery(val msisdn: String) : Query
data class SystemFindUserByEmailQuery(val email: String) : Query

// Events
data class UserFoundEvent(val userDto: UserDto) : Event()
data class SystemUserFoundEvent(val userDetailsDto: UserDetailsWithId) : Event()

// Error
data class UserNotFoundError(override val message: String) : WorkflowError(message)

interface FindUserApi {
  suspend fun findUserById(query: FindUserByIdQuery): Either<WorkflowError, UserFoundEvent>
  suspend fun findUserByEmail(query: FindUserByEmailQuery): Either<WorkflowError, UserFoundEvent>
  suspend fun findUserByMsisdn(query: FindUserByMsisdnQuery): Either<WorkflowError, UserFoundEvent>
}
