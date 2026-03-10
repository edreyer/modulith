package io.liquidsoftware.base.user.application.workflows

import arrow.core.Either
import arrow.core.flatMap
import io.liquidsoftware.base.user.application.mapper.toUserDetailsWithId
import io.liquidsoftware.base.user.application.port.`in`.SystemFindUserByEmailQuery
import io.liquidsoftware.base.user.application.port.`in`.SystemUserFoundEvent
import io.liquidsoftware.base.user.application.port.`in`.UserNotFoundError
import io.liquidsoftware.base.user.application.port.out.FindUserPort
import io.liquidsoftware.base.user.domain.User
import io.liquidsoftware.common.application.error.ApplicationError
import io.liquidsoftware.common.application.error.toApplicationUseCaseEither
import io.liquidsoftware.common.security.UserDetailsWithId
import io.liquidsoftware.common.usecase.Query as UseCaseQuery
import io.liquidsoftware.common.usecase.Workflow as UseCaseWorkflow
import io.liquidsoftware.common.usecase.WorkflowContext
import io.liquidsoftware.common.usecase.WorkflowResult
import io.liquidsoftware.common.usecase.WorkflowState
import io.liquidsoftware.common.usecase.toUseCaseEither
import io.liquidsoftware.common.usecase.useCase
import io.liquidsoftware.workflow.WorkflowError as UseCaseError

internal class SystemFindUserByEmailUseCase(
  private val findUserPort: FindUserPort,
) {

  private val useCase = useCase<SystemFindUserLookupQuery> {
    startWith { query -> Either.Right(SystemLookupState(query.email)) }
    then(LoadUserByEmailStep("load-user-by-email", findUserPort))
    then(MapUserToUserDetailsStep("map-user-to-user-details"))
  }

  suspend fun execute(query: SystemFindUserByEmailQuery): Either<ApplicationError, SystemUserFoundEvent> =
    useCase
      .executeProjected(
        SystemFindUserLookupQuery(query.email),
        projector = { result ->
        result
          .requireState<SystemUserDetailsState>(USE_CASE_NAME)
          .fold(
            { Either.Left(it) },
            { state -> Either.Right(SystemUserFoundEvent(state.userDetails)) },
          )
        },
      ).toApplicationUseCaseEither { domainError ->
          when (domainError.code) {
            USER_NOT_FOUND_CODE -> UserNotFoundError(domainError.message)
            else -> null
          }
        }

  private class LoadUserByEmailStep(
    override val id: String,
    private val findUserPort: FindUserPort,
  ) : UseCaseWorkflow<SystemLookupState, FoundUserState>() {

    override suspend fun executeWorkflow(
      input: SystemLookupState,
      context: WorkflowContext,
    ): Either<UseCaseError, WorkflowResult<FoundUserState>> =
      findUserPort
        .findUserByEmail(input.email)
        .toUseCaseEither()
        .flatMap { user ->
          user
            ?.let { Either.Right(WorkflowResult(FoundUserState(it), context = context)) }
            ?: Either.Left(UseCaseError.DomainError(USER_NOT_FOUND_CODE, input.email))
        }
  }

  private class MapUserToUserDetailsStep(
    override val id: String,
  ) : UseCaseWorkflow<FoundUserState, SystemUserDetailsState>() {

    override suspend fun executeWorkflow(
      input: FoundUserState,
      context: WorkflowContext,
    ): Either<UseCaseError, WorkflowResult<SystemUserDetailsState>> =
      Either.Right(
        WorkflowResult(
          state = SystemUserDetailsState(input.user.toUserDetailsWithId()),
          context = context,
        )
      )
  }

  private data class SystemFindUserLookupQuery(
    val email: String,
  ) : UseCaseQuery

  private data class SystemLookupState(
    val email: String,
  ) : WorkflowState

  private data class FoundUserState(
    val user: User,
  ) : WorkflowState

  private data class SystemUserDetailsState(
    val userDetails: UserDetailsWithId,
  ) : WorkflowState
}

private const val USE_CASE_NAME = "SystemFindUserByEmailUseCase"
private const val USER_NOT_FOUND_CODE = "USER_NOT_FOUND"
