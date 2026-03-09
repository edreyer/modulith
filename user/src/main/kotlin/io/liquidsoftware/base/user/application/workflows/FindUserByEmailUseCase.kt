package io.liquidsoftware.base.user.application.workflows

import arrow.core.Either
import io.liquidsoftware.base.user.application.port.`in`.FindUserByEmailQuery
import io.liquidsoftware.base.user.application.port.`in`.UserFoundEvent
import io.liquidsoftware.base.user.application.port.out.FindUserPort
import io.liquidsoftware.base.user.domain.User
import io.liquidsoftware.common.workflow.WorkflowError as LegacyWorkflowError

internal class FindUserByEmailUseCase(
  findUserPort: FindUserPort,
) : UserLookupUseCase(findUserPort, "find-user-by-email") {
  suspend fun execute(query: FindUserByEmailQuery): Either<LegacyWorkflowError, UserFoundEvent> =
    executeLookup(query.email)

  override suspend fun loadUser(
    findUserPort: FindUserPort,
    lookupValue: String,
  ): Either<LegacyWorkflowError, User?> = findUserPort.findUserByEmail(lookupValue)
}
