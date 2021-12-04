package io.liquidsoftware.base.user.application.port.`in`

import io.liquidsoftware.common.security.UserDetailsWithId
import io.liquidsoftware.common.workflow.Event
import io.liquidsoftware.common.workflow.Query

// Inputs
data class FindUserByIdQuery(val userId: String) : Query
data class FindUserByEmailQuery(val email: String) : Query
data class FindUserByMsisdnQuery(val msisdn: String) : Query

data class SystemFindUserByEmailQuery(val email: String) : Query

// Events
data class UserFoundEvent(val userDto: UserDto) : Event()

data class SystemUserFoundEvent(val userDetailsDto: UserDetailsWithId) : Event()


// Error
data class UserNotFoundError(override val message: String) : RuntimeException(message)


