package io.liquidsoftware.base.user.application.workflows

import arrow.core.Either
import arrow.core.flatMap
import arrow.core.nonEmptyListOf
import arrow.core.raise.either
import io.liquidsoftware.base.user.application.mapper.toUserDto
import io.liquidsoftware.base.user.application.port.`in`.RegisterUserCommand
import io.liquidsoftware.base.user.application.port.`in`.UserExistsError
import io.liquidsoftware.base.user.application.port.`in`.UserRegisteredEvent
import io.liquidsoftware.base.user.application.port.out.FindUserPort
import io.liquidsoftware.base.user.application.port.out.UserEventPort
import io.liquidsoftware.base.user.domain.Role
import io.liquidsoftware.base.user.domain.UnregisteredUser
import io.liquidsoftware.common.application.error.ApplicationError
import io.liquidsoftware.common.application.error.toApplicationUseCaseEither
import io.liquidsoftware.common.security.runAsSuperUser
import io.liquidsoftware.common.types.ValidationError
import io.liquidsoftware.common.usecase.Workflow as UseCaseWorkflow
import io.liquidsoftware.common.usecase.WorkflowContext
import io.liquidsoftware.common.usecase.WorkflowResult
import io.liquidsoftware.common.usecase.WorkflowState
import io.liquidsoftware.common.usecase.toUseCaseEither
import io.liquidsoftware.common.usecase.toUseCaseError
import io.liquidsoftware.common.usecase.useCase
import io.liquidsoftware.common.workflow.WorkflowValidationError
import io.liquidsoftware.workflow.WorkflowError as UseCaseError
import org.springframework.security.crypto.password.PasswordEncoder

internal class RegisterUserUseCase(
  private val passwordEncoder: PasswordEncoder,
  private val findUserPort: FindUserPort,
  private val userEventPort: UserEventPort,
) {

  private val useCase = useCase<RegisterUserCommand> {
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

  suspend fun execute(command: RegisterUserCommand): Either<ApplicationError, UserRegisteredEvent> =
    runAsSuperUser {
      useCase.executeProjected(
        command,
        projector = { result ->
          result.requireState<RegisteredUserState>("persist-registered-user").fold(
            { Either.Left(it) },
            { state -> Either.Right(state.event) },
          )
        },
      ).toApplicationUseCaseEither { domainError ->
        when (domainError.code) {
          USER_EXISTS_CODE -> UserExistsError(domainError.message)
          else -> null
        }
      }
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

private fun validationError(message: String): UseCaseError =
  WorkflowValidationError(nonEmptyListOf(ValidationError(message))).toUseCaseError()

private const val USER_EXISTS_CODE = "USER_EXISTS"
