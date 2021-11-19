package ventures.dvx.base.user.application.port.`in`

import ventures.dvx.common.workflow.BaseSafeSecureWorkflow
import ventures.dvx.common.workflow.BaseSafeWorkflow
import ventures.dvx.common.workflow.Event
import ventures.dvx.common.workflow.Query

// Inputs
data class FindUserByIdQuery(val userId: String) : Query
data class FindUserByEmailQuery(val email: String) : Query
data class FindUserByMsisdnQuery(val msisdn: String) : Query

internal data class SystemFindUserByEmailQuery(val email: String) : Query

// Outputs
data class UserFoundEvent(val userDto: UserDto) : Event()

internal data class SystemUserFoundEvent(val userDetailsDto: UserDetailsDto) : Event()

// Error
data class UserNotFoundError(override val message: String) : RuntimeException(message)

// Workflows
abstract class FindUserByIdWorkflow :
  BaseSafeWorkflow<FindUserByIdQuery, UserFoundEvent>(),
  UserSecured<FindUserByIdQuery>

abstract class FindUserByEmailWorkflow :
  BaseSafeSecureWorkflow<FindUserByEmailQuery, UserFoundEvent>(),
  UserSecured<FindUserByEmailQuery>

abstract class FindUserByMsisdnWorkflow :
  BaseSafeSecureWorkflow<FindUserByMsisdnQuery, UserFoundEvent>(),
  UserSecured<FindUserByMsisdnQuery>

internal abstract class SystemFindUserByEmailWorkflow :
  BaseSafeSecureWorkflow<SystemFindUserByEmailQuery, SystemUserFoundEvent>(),
  UserSecured<SystemFindUserByEmailQuery>

