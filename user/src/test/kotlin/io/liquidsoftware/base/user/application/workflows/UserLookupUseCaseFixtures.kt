package io.liquidsoftware.base.user.application.workflows

import arrow.core.Either
import arrow.core.raise.either
import io.liquidsoftware.base.user.application.port.out.FindUserPort
import io.liquidsoftware.base.user.domain.ActiveUser
import io.liquidsoftware.base.user.domain.AdminUser
import io.liquidsoftware.base.user.domain.User
import io.liquidsoftware.common.workflow.WorkflowError

internal fun findUserPort(
  findById: suspend (String) -> User? = { null },
  findByEmail: suspend (String) -> User? = { null },
  findByMsisdn: suspend (String) -> User? = { null },
): FindUserPort = object : FindUserPort {
  override suspend fun findUserById(userId: String): Either<WorkflowError, User?> =
    Either.Right(findById(userId))

  override suspend fun findUserByEmail(email: String): Either<WorkflowError, User?> =
    Either.Right(findByEmail(email))

  override suspend fun findUserByMsisdn(msisdn: String): Either<WorkflowError, User?> =
    Either.Right(findByMsisdn(msisdn))
}

internal fun activeUser(): ActiveUser =
  either {
    ActiveUser.of(
      id = "u_active",
      msisdn = "+15125550111",
      email = "user@liquidsoftware.io",
      encryptedPassword = "encoded-password",
    )
  }.fold({ error("invalid active user") }, { it })

internal fun adminUser(): AdminUser =
  either {
    AdminUser.of(
      id = "u_admin",
      msisdn = "+15125550112",
      email = "admin@liquidsoftware.io",
      encryptedPassword = "encoded-password",
    )
  }.fold({ error("invalid admin user") }, { it })
