package io.liquidsoftware.base.user.application.port.out

import arrow.core.Either
import io.liquidsoftware.base.user.domain.User
import io.liquidsoftware.common.workflow.WorkflowError

internal interface FindUserPort {
  suspend fun findUserById(userId: String): Either<WorkflowError, User?>
  suspend fun findUserByEmail(email: String): Either<WorkflowError, User?>
  suspend fun findUserByMsisdn(msisdn: String): Either<WorkflowError, User?>
}
