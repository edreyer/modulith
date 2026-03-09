package io.liquidsoftware.base.user.application.workflows

import arrow.core.Either
import arrow.core.flatMap
import arrow.core.nonEmptyListOf
import arrow.core.raise.either
import io.liquidsoftware.base.user.application.mapper.toUserDto
import io.liquidsoftware.base.user.application.port.`in`.DisableUserCommand
import io.liquidsoftware.base.user.application.port.`in`.EnableUserCommand
import io.liquidsoftware.base.user.application.port.`in`.RegisterUserCommand
import io.liquidsoftware.base.user.application.port.`in`.UserDisabledEvent
import io.liquidsoftware.base.user.application.port.`in`.UserDto
import io.liquidsoftware.base.user.application.port.`in`.UserEnabledEvent
import io.liquidsoftware.base.user.application.port.`in`.UserEvent
import io.liquidsoftware.base.user.application.port.`in`.UserExistsError
import io.liquidsoftware.base.user.application.port.`in`.UserNotFoundError
import io.liquidsoftware.base.user.application.port.`in`.UserRegisteredEvent
import io.liquidsoftware.base.user.application.port.out.FindUserPort
import io.liquidsoftware.base.user.application.port.out.UserEventPort
import io.liquidsoftware.base.user.domain.Role
import io.liquidsoftware.base.user.domain.UnregisteredUser
import io.liquidsoftware.base.user.domain.User
import io.liquidsoftware.common.security.runAsSuperUser
import io.liquidsoftware.common.types.ValidationError
import io.liquidsoftware.common.usecase.Command as UseCaseCommand
import io.liquidsoftware.common.usecase.Workflow as UseCaseWorkflow
import io.liquidsoftware.common.usecase.WorkflowContext
import io.liquidsoftware.common.usecase.WorkflowResult
import io.liquidsoftware.common.usecase.WorkflowState
import io.liquidsoftware.common.usecase.legacy.executeLegacyProjected
import io.liquidsoftware.common.usecase.legacy.toUseCaseEither
import io.liquidsoftware.common.usecase.legacy.toUseCaseError
import io.liquidsoftware.common.usecase.useCase
import io.liquidsoftware.common.workflow.WorkflowError as LegacyWorkflowError
import io.liquidsoftware.common.workflow.WorkflowValidationError
import io.liquidsoftware.workflow.WorkflowError as UseCaseError
import org.springframework.security.crypto.password.PasswordEncoder

internal class RegisterUserUseCase(
  private val passwordEncoder: PasswordEncoder,
  private val findUserPort: FindUserPort,
  private val userEventPort: UserEventPort,
) {

  private val useCase = useCase<RegisterUserRequest> {
    startWith { request ->
      Either.Right(
        RegisterUserState(
          msisdn = request.msisdn,
          email = request.email,
          password = request.password,
          role = request.role,
        )
      )
    }
    then(EnsureEmailAvailableStep("ensure-email-available", findUserPort))
    then(BuildUnregisteredUserStep("build-unregistered-user", passwordEncoder))
    then(PersistRegisteredUserStep("persist-registered-user", userEventPort))
  }

  suspend fun execute(command: RegisterUserCommand): Either<LegacyWorkflowError, UserRegisteredEvent> =
    runAsSuperUser {
      useCase.executeLegacyProjected(
        request = command,
        requestMapper = { request ->
          RegisterUserRequest(
            msisdn = request.msisdn,
            email = request.email,
            password = request.password,
            role = request.role,
          )
        },
        projector = { result ->
          result.requireState<RegisteredUserState>("persist-registered-user").fold(
            { Either.Left(it) },
            { state -> Either.Right(state.event) },
          )
        },
        domainErrorMapper = { domainError ->
          when (domainError.code) {
            USER_EXISTS_CODE -> UserExistsError(domainError.message)
            else -> null
          }
        },
      )
    }

  private class EnsureEmailAvailableStep(
    override val id: String,
    private val findUserPort: FindUserPort,
  ) : UseCaseWorkflow<RegisterUserState, RegisterUserState>() {

    override suspend fun executeWorkflow(
      input: RegisterUserState,
      context: WorkflowContext,
    ): Either<UseCaseError, WorkflowResult<RegisterUserState>> =
      findUserPort.findUserByEmail(input.email)
        .toUseCaseEither()
        .flatMap { existingUser ->
          if (existingUser != null) {
            Either.Left(UseCaseError.DomainError(USER_EXISTS_CODE, "User ${input.msisdn} exists"))
          } else {
            Either.Right(WorkflowResult(state = input, context = context))
          }
        }
  }

  private class BuildUnregisteredUserStep(
    override val id: String,
    private val passwordEncoder: PasswordEncoder,
  ) : UseCaseWorkflow<RegisterUserState, UnregisteredUserState>() {

    override suspend fun executeWorkflow(
      input: RegisterUserState,
      context: WorkflowContext,
    ): Either<UseCaseError, WorkflowResult<UnregisteredUserState>> {
      val encodedPassword = passwordEncoder.encode(input.password)
        ?: return Either.Left(UseCaseError.ExecutionError("Password encoder returned null"))

      val role = Role.entries.find { it.name == input.role }
        ?: return Either.Left(validationError("Invalid role ${input.role}"))

      return either {
        UnregisteredUser.of(
          msisdn = input.msisdn,
          email = input.email,
          encryptedPassword = encodedPassword,
          role = role,
        )
      }.fold(
        { Either.Left(WorkflowValidationError(it).toUseCaseError()) },
        { user -> Either.Right(WorkflowResult(state = UnregisteredUserState(user), context = context)) },
      )
    }
  }

  private class PersistRegisteredUserStep(
    override val id: String,
    private val userEventPort: UserEventPort,
  ) : UseCaseWorkflow<UnregisteredUserState, RegisteredUserState>() {

    override suspend fun executeWorkflow(
      input: UnregisteredUserState,
      context: WorkflowContext,
    ): Either<UseCaseError, WorkflowResult<RegisteredUserState>> =
      userEventPort.handle(
        UserRegisteredEvent(
          userDto = input.user.toUserDto(),
          password = input.user.encryptedPassword.value,
        )
      )
        .toUseCaseEither()
        .map { event -> WorkflowResult(state = RegisteredUserState(event), context = context) }
  }
}

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

  protected suspend fun executeUserAdmin(userId: String): Either<LegacyWorkflowError, T> =
    useCase.executeLegacyProjected(
      request = UserIdRequest(userId),
      requestMapper = { it },
      projector = { result ->
        result.requireState<PersistedUserEventState>(workflowId).fold(
          { Either.Left(it) },
          { state -> Either.Right(toEvent(state.userDto)) },
        )
      },
      domainErrorMapper = { domainError ->
        when (domainError.code) {
          USER_NOT_FOUND_CODE -> UserNotFoundError(domainError.message)
          else -> null
        }
      },
    )

  private class LoadUserByIdStep(
    override val id: String,
    private val findUserPort: FindUserPort,
  ) : UseCaseWorkflow<UserIdState, FoundUserState>() {

    override suspend fun executeWorkflow(
      input: UserIdState,
      context: WorkflowContext,
    ): Either<UseCaseError, WorkflowResult<FoundUserState>> =
      findUserPort.findUserById(input.userId)
        .toUseCaseEither()
        .flatMap { user ->
          user
            ?.let { Either.Right(WorkflowResult(state = FoundUserState(it), context = context)) }
            ?: Either.Left(
              UseCaseError.DomainError(USER_NOT_FOUND_CODE, "User not found with ID ${input.userId}")
            )
        }
  }

  private class PersistUserAdminEventStep<T : UserEvent>(
    override val id: String,
    private val userEventPort: UserEventPort,
    private val eventFactory: (UserDto) -> T,
  ) : UseCaseWorkflow<FoundUserState, PersistedUserEventState>() {

    override suspend fun executeWorkflow(
      input: FoundUserState,
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

internal class EnableUserUseCase(
  findUserPort: FindUserPort,
  userEventPort: UserEventPort,
) : UserAdminUseCase<UserEnabledEvent>(
  findUserPort = findUserPort,
  userEventPort = userEventPort,
  workflowId = "persist-user-enabled",
) {
  suspend fun execute(command: EnableUserCommand): Either<LegacyWorkflowError, UserEnabledEvent> =
    executeUserAdmin(command.userId)

  override fun toEvent(userDto: UserDto): UserEnabledEvent = UserEnabledEvent(userDto)
}

internal class DisableUserUseCase(
  findUserPort: FindUserPort,
  userEventPort: UserEventPort,
) : UserAdminUseCase<UserDisabledEvent>(
  findUserPort = findUserPort,
  userEventPort = userEventPort,
  workflowId = "persist-user-disabled",
) {
  suspend fun execute(command: DisableUserCommand): Either<LegacyWorkflowError, UserDisabledEvent> =
    executeUserAdmin(command.userId)

  override fun toEvent(userDto: UserDto): UserDisabledEvent = UserDisabledEvent(userDto)
}

private data class RegisterUserRequest(
  val msisdn: String,
  val email: String,
  val password: String,
  val role: String,
) : UseCaseCommand

private data class RegisterUserState(
  val msisdn: String,
  val email: String,
  val password: String,
  val role: String,
) : WorkflowState

private data class UnregisteredUserState(
  val user: UnregisteredUser,
) : WorkflowState

private data class RegisteredUserState(
  val event: UserRegisteredEvent,
) : WorkflowState

private data class UserIdRequest(
  val userId: String,
) : UseCaseCommand

private data class UserIdState(
  val userId: String,
) : WorkflowState

private data class FoundUserState(
  val user: User,
) : WorkflowState

private data class PersistedUserEventState(
  val userDto: UserDto,
) : WorkflowState

private fun validationError(message: String): UseCaseError =
  WorkflowValidationError(nonEmptyListOf(ValidationError(message))).toUseCaseError()

private const val USER_EXISTS_CODE = "USER_EXISTS"
private const val USER_NOT_FOUND_CODE = "USER_NOT_FOUND"
