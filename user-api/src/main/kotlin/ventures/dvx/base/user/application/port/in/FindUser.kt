package ventures.dvx.base.user.application.port.`in`

import ventures.dvx.common.workflow.Event
import ventures.dvx.common.workflow.Query

// Inputs
data class FindUserByIdQuery(val userId: String) : Query
data class FindUserByEmailQuery(val email: String) : Query
data class FindUserByMsisdnQuery(val msisdn: String) : Query

// Outputs
data class UserFoundEvent(val userDto: UserDto) : Event()


// Error
data class UserNotFoundError(override val message: String) : RuntimeException(message)


