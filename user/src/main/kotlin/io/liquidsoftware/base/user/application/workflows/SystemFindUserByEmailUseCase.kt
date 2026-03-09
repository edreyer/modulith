package io.liquidsoftware.base.user.application.workflows

import arrow.core.Either
import arrow.core.flatMap
import io.liquidsoftware.base.user.application.mapper.toUserDetailsWithId
import io.liquidsoftware.base.user.application.port.`in`.SystemFindUserByEmailQuery
import io.liquidsoftware.base.user.application.port.`in`.SystemUserFoundEvent
import io.liquidsoftware.base.user.application.port.`in`.UserNotFoundError
import io.liquidsoftware.base.user.application.port.out.FindUserPort
import io.liquidsoftware.base.user.domain.User
import io.liquidsoftware.common.security.UserDetailsWithId
import io.liquidsoftware.common.usecase.Query as UseCaseQuery
import io.liquidsoftware.common.usecase.Workflow as UseCaseWorkflow
import io.liquidsoftware.common.usecase.WorkflowContext
import io.liquidsoftware.common.usecase.WorkflowResult
import io.liquidsoftware.common.usecase.WorkflowState
import io.liquidsoftware.common.usecase.useCase
import io.liquidsoftware.common.workflow.ServerError
import io.liquidsoftware.common.workflow.UnauthorizedWorkflowError
import io.liquidsoftware.common.workflow.WorkflowError as LegacyWorkflowError
import io.liquidsoftware.workflow.WorkflowError as UseCaseError
import org.springframework.stereotype.Component

@Component
internal class SystemFindUserByEmailUseCase(
  private val findUserPort: FindUserPort,
) {

  private val useCase = useCase<SystemFindUserLookupQuery> {
    startWith { query -> Either.Right(SystemLookupState(query.email)) }
    then(LoadUserByEmailStep("load-user-by-email", findUserPort))
    then(MapUserToUserDetailsStep("map-user-to-user-details"))
  }

  suspend fun execute(query: SystemFindUserByEmailQuery): Either<LegacyWorkflowError, SystemUserFoundEvent> =
    useCase
      .executeProjected(SystemFindUserLookupQuery(query.email)) { result ->
        result
          .requireState<SystemUserDetailsState>(USE_CASE_NAME)
          .fold(
            { Either.Left(it) },
            { state -> Either.Right(SystemUserFoundEvent(state.userDetails)) },
          )
      }
      .fold(
        { Either.Left(it.toLegacyWorkflowError()) },
        { Either.Right(it) },
      )

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
        .fold(
          { Either.Left(it.toUseCaseError()) },
          { user ->
            user
              ?.let { Either.Right(WorkflowResult(FoundUserState(it), context = context)) }
              ?: Either.Left(UseCaseError.DomainError(USER_NOT_FOUND_CODE, input.email))
          },
        )
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

private fun LegacyWorkflowError.toUseCaseError(): UseCaseError = when (this) {
  is UnauthorizedWorkflowError -> UseCaseError.DomainError(UNAUTHORIZED_CODE, message)
  is UserNotFoundError -> UseCaseError.DomainError(USER_NOT_FOUND_CODE, message)
  is ServerError -> UseCaseError.ExecutionError(msg)
  else -> UseCaseError.ExecutionError(message)
}

private fun UseCaseError.toLegacyWorkflowError(): LegacyWorkflowError = when (this) {
  is UseCaseError.DomainError -> when (code) {
    USER_NOT_FOUND_CODE -> UserNotFoundError(message)
    UNAUTHORIZED_CODE -> UnauthorizedWorkflowError(message)
    else -> ServerError(message)
  }
  is UseCaseError.ValidationError -> ServerError(message)
  is UseCaseError.ExecutionError -> ServerError(message)
  is UseCaseError.ExceptionError -> ServerError("$message: ${ex.message ?: ex::class.simpleName}")
  is UseCaseError.CompositionError -> ServerError(message)
  is UseCaseError.ExecutionContextError -> error.toLegacyWorkflowError()
  is UseCaseError.ChainError -> error.toLegacyWorkflowError()
}

private const val USE_CASE_NAME = "SystemFindUserByEmailUseCase"
private const val USER_NOT_FOUND_CODE = "USER_NOT_FOUND"
private const val UNAUTHORIZED_CODE = "UNAUTHORIZED"
