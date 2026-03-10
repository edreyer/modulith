package io.liquidsoftware.base.user.application.workflows

import arrow.core.Either
import io.liquidsoftware.base.user.application.port.`in`.FindUserByMsisdnQuery
import io.liquidsoftware.base.user.application.port.`in`.UserFoundEvent
import io.liquidsoftware.base.user.application.port.out.FindUserPort
import io.liquidsoftware.base.user.domain.User
import io.liquidsoftware.common.application.error.ApplicationError
import io.liquidsoftware.common.workflow.WorkflowError as LegacyWorkflowError

internal class FindUserByMsisdnUseCase(
  findUserPort: FindUserPort,
) : UserLookupUseCase(findUserPort, "find-user-by-msisdn") {
  suspend fun execute(query: FindUserByMsisdnQuery): Either<ApplicationError, UserFoundEvent> =
    executeLookup(query.msisdn)

  override suspend fun loadUser(
    findUserPort: FindUserPort,
    lookupValue: String,
  ): Either<LegacyWorkflowError, User?> = findUserPort.findUserByMsisdn(lookupValue)
}
