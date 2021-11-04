package ventures.dvx.base.user.application.port.`in`

import ventures.dvx.common.workflow.Event
import ventures.dvx.common.workflow.Query
import ventures.dvx.common.workflow.SafeWorkflow

// Inputs
@JvmInline
value class FindUserByEmailQuery(val email: String) : Query
@JvmInline
value class FindUserByMsisdnQuery(val msisdn: String) : Query
// Outputs

data class FindUserEvent(val userDto: UserDto) : Event

// Error

data class UserNotFoundError(val lookupKey: String) : RuntimeException()

// Use Case

interface FindUserWorkflow : SafeWorkflow<FindUserByEmailQuery, FindUserEvent>
