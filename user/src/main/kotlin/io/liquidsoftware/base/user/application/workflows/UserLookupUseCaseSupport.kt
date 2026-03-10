package io.liquidsoftware.base.user.application.workflows

import arrow.core.Either
import arrow.core.flatMap
import io.liquidsoftware.base.user.application.mapper.toUserDto
import io.liquidsoftware.base.user.application.port.`in`.UserFoundEvent
import io.liquidsoftware.base.user.application.port.`in`.UserNotFoundError
import io.liquidsoftware.base.user.application.port.out.FindUserPort
import io.liquidsoftware.base.user.domain.User
import io.liquidsoftware.common.application.error.ApplicationError
import io.liquidsoftware.common.application.error.toApplicationUseCaseEither
import io.liquidsoftware.common.usecase.Query as UseCaseQuery
import io.liquidsoftware.common.usecase.Workflow as UseCaseWorkflow
import io.liquidsoftware.common.usecase.WorkflowContext
import io.liquidsoftware.common.usecase.WorkflowResult
import io.liquidsoftware.common.usecase.WorkflowState
import io.liquidsoftware.common.usecase.toUseCaseEither
import io.liquidsoftware.common.usecase.useCase
import io.liquidsoftware.common.workflow.WorkflowError as LegacyWorkflowError
import io.liquidsoftware.workflow.WorkflowError as UseCaseError

internal abstract class UserLookupUseCase(
  private val findUserPort: FindUserPort,
  private val workflowId: String,
) {

  private val useCase = useCase<LookupQuery> {
    startWith { query -> Either.Right(LookupState(query.lookupValue)) }
    then(LoadUserStep(workflowId, findUserPort, ::loadUser))
    then(MapUserToDtoStep("map-user-to-dto"))
  }

  protected abstract suspend fun loadUser(findUserPort: FindUserPort, lookupValue: String): Either<LegacyWorkflowError, User?>

  protected suspend fun executeLookup(lookupValue: String): Either<ApplicationError, UserFoundEvent> =
    useCase.executeProjected(
      LookupQuery(lookupValue),
      projector = { result ->
        result
          .requireState<FoundUserDtoState>(workflowId)
          .fold(
            { Either.Left(it) },
            { state -> Either.Right(UserFoundEvent(state.userDto)) },
          )
      },
    ).toApplicationUseCaseEither { domainError ->
      when (domainError.code) {
        USER_NOT_FOUND_CODE -> UserNotFoundError(domainError.message)
        else -> null
      }
    }

  private class LoadUserStep(
    override val id: String,
    private val findUserPort: FindUserPort,
    private val loader: suspend (FindUserPort, String) -> Either<LegacyWorkflowError, User?>,
  ) : UseCaseWorkflow<LookupState, FoundUserState>() {

    override suspend fun executeWorkflow(
      input: LookupState,
      context: WorkflowContext,
    ): Either<UseCaseError, WorkflowResult<FoundUserState>> =
      loader(findUserPort, input.lookupValue)
        .toUseCaseEither()
        .flatMap { user ->
          user
            ?.let { Either.Right(WorkflowResult(FoundUserState(it), context = context)) }
            ?: Either.Left(UseCaseError.DomainError(USER_NOT_FOUND_CODE, input.lookupValue))
        }
  }

  private class MapUserToDtoStep(
    override val id: String,
  ) : UseCaseWorkflow<FoundUserState, FoundUserDtoState>() {

    override suspend fun executeWorkflow(
      input: FoundUserState,
      context: WorkflowContext,
    ): Either<UseCaseError, WorkflowResult<FoundUserDtoState>> =
      Either.Right(
        WorkflowResult(
          state = FoundUserDtoState(input.user.toUserDto()),
          context = context,
        )
      )
  }
}

private data class LookupQuery(
  val lookupValue: String,
) : UseCaseQuery

private data class LookupState(
  val lookupValue: String,
) : WorkflowState

private data class FoundUserState(
  val user: User,
) : WorkflowState

private data class FoundUserDtoState(
  val userDto: io.liquidsoftware.base.user.application.port.`in`.UserDto,
) : WorkflowState

private const val USER_NOT_FOUND_CODE = "USER_NOT_FOUND"
