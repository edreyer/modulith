package io.liquidsoftware.base.user.application.workflows

import arrow.core.Either
import arrow.core.flatMap
import io.liquidsoftware.base.user.application.mapper.toUserDto
import io.liquidsoftware.base.user.application.port.`in`.UserDto
import io.liquidsoftware.base.user.application.port.`in`.UserEvent
import io.liquidsoftware.base.user.application.port.`in`.UserNotFoundError
import io.liquidsoftware.base.user.application.port.out.FindUserPort
import io.liquidsoftware.base.user.application.port.out.UserEventPort
import io.liquidsoftware.base.user.domain.User
import io.liquidsoftware.common.application.error.ApplicationError
import io.liquidsoftware.common.application.error.toApplicationUseCaseEither
import io.liquidsoftware.common.usecase.Command as UseCaseCommand
import io.liquidsoftware.common.usecase.Workflow as UseCaseWorkflow
import io.liquidsoftware.common.usecase.WorkflowContext
import io.liquidsoftware.common.usecase.WorkflowResult
import io.liquidsoftware.common.usecase.WorkflowState
import io.liquidsoftware.common.usecase.toUseCaseEither
import io.liquidsoftware.common.usecase.useCase
import io.liquidsoftware.workflow.WorkflowError as UseCaseError

internal abstract class UserAdminUseCase<T : UserEvent>(
  private val findUserPort: FindUserPort,
  private val userEventPort: UserEventPort,
  private val workflowId: String,
) {

  private val useCase = useCase<UserIdRequest> {
    startWith { request -> Either.Right(UserIdState(request.userId)) }
    then(LoadUserByIdStep("load-user-by-id", findUserPort))
    then(PersistUserAdminEventStep(workflowId, userEventPort, ::toEvent))
  }

  protected abstract fun toEvent(userDto: UserDto): T

  protected suspend fun executeUserAdmin(userId: String): Either<ApplicationError, T> =
    useCase.executeProjected(
      UserIdRequest(userId),
      projector = { result ->
        result.requireState<PersistedUserEventState>(workflowId).fold(
          { Either.Left(it) },
          { state -> Either.Right(toEvent(state.userDto)) },
        )
      },
    ).toApplicationUseCaseEither { domainError ->
      when (domainError.code) {
        USER_NOT_FOUND_CODE -> UserNotFoundError(domainError.message)
        else -> null
      }
    }

  private class LoadUserByIdStep(
    override val id: String,
    private val findUserPort: FindUserPort,
  ) : UseCaseWorkflow<UserIdState, AdminFoundUserState>() {

    override suspend fun executeWorkflow(
      input: UserIdState,
      context: WorkflowContext,
    ): Either<UseCaseError, WorkflowResult<AdminFoundUserState>> =
      findUserPort.findUserById(input.userId)
        .toUseCaseEither()
        .flatMap { user ->
          user
            ?.let { Either.Right(WorkflowResult(state = AdminFoundUserState(it), context = context)) }
            ?: Either.Left(
              UseCaseError.DomainError(USER_NOT_FOUND_CODE, "User not found with ID ${input.userId}")
            )
        }
  }

  private class PersistUserAdminEventStep<T : UserEvent>(
    override val id: String,
    private val userEventPort: UserEventPort,
    private val eventFactory: (UserDto) -> T,
  ) : UseCaseWorkflow<AdminFoundUserState, PersistedUserEventState>() {

    override suspend fun executeWorkflow(
      input: AdminFoundUserState,
      context: WorkflowContext,
    ): Either<UseCaseError, WorkflowResult<PersistedUserEventState>> =
      userEventPort.handle(eventFactory(input.user.toUserDto()))
        .toUseCaseEither()
        .map { event ->
          WorkflowResult(
            state = PersistedUserEventState(event.userDto),
            context = context,
          )
        }
  }
}

private data class UserIdRequest(
  val userId: String,
) : UseCaseCommand

private data class UserIdState(
  val userId: String,
) : WorkflowState

private data class AdminFoundUserState(
  val user: User,
) : WorkflowState

private data class PersistedUserEventState(
  val userDto: UserDto,
) : WorkflowState

private const val USER_NOT_FOUND_CODE = "USER_NOT_FOUND"
