package io.liquidsoftware.base.user.application.workflows

import arrow.core.Either
import io.liquidsoftware.base.user.application.port.`in`.FindUserByIdQuery
import io.liquidsoftware.base.user.application.port.`in`.UserFoundEvent
import io.liquidsoftware.base.user.application.port.out.FindUserPort
import io.liquidsoftware.base.user.domain.User
import io.liquidsoftware.common.workflow.WorkflowError as LegacyWorkflowError

internal class FindUserByIdUseCase(
  findUserPort: FindUserPort,
) : UserLookupUseCase(findUserPort, "find-user-by-id") {
  suspend fun execute(query: FindUserByIdQuery): Either<LegacyWorkflowError, UserFoundEvent> =
    executeLookup(query.userId)

  override suspend fun loadUser(
    findUserPort: FindUserPort,
    lookupValue: String,
  ): Either<LegacyWorkflowError, User?> = findUserPort.findUserById(lookupValue)
}
