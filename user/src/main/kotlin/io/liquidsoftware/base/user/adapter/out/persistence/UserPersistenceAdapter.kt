package io.liquidsoftware.base.user.adapter.out.persistence

import arrow.core.Either
import arrow.core.left
import arrow.core.raise.Raise
import arrow.core.raise.context.raise
import arrow.core.raise.either
import arrow.core.right
import arrow.core.toNonEmptyListOrNull
import io.liquidsoftware.base.user.application.port.`in`.RoleDto
import io.liquidsoftware.base.user.application.port.`in`.UserEvent
import io.liquidsoftware.base.user.application.port.`in`.UserRegisteredEvent
import io.liquidsoftware.base.user.application.port.out.FindUserPort
import io.liquidsoftware.base.user.application.port.out.UserEventPort
import io.liquidsoftware.base.user.domain.ActiveUser
import io.liquidsoftware.base.user.domain.AdminUser
import io.liquidsoftware.base.user.domain.DisabledUser
import io.liquidsoftware.base.user.domain.Role
import io.liquidsoftware.base.user.domain.User
import io.liquidsoftware.common.ext.toWorkflowError
import io.liquidsoftware.common.ext.withContextIO
import io.liquidsoftware.common.ext.workflowBoundary
import io.liquidsoftware.common.logging.LoggerDelegate
import io.liquidsoftware.common.security.acl.AclChecker
import io.liquidsoftware.common.security.acl.Permission
import io.liquidsoftware.common.types.ValidationError
import io.liquidsoftware.common.types.ValidationErrors
import io.liquidsoftware.common.workflow.WorkflowError
import io.liquidsoftware.common.workflow.WorkflowValidationError

internal class UserPersistenceAdapter(
  private val userRepository: UserRepository,
  private val ac: AclChecker
) : FindUserPort, UserEventPort {

  private val logger by LoggerDelegate()

  override suspend fun findUserById(userId: String): Either<WorkflowError, User?> =
    findUser { userRepository.findByUserId(userId) }

  override suspend fun findUserByMsisdn(msisdn: String): Either<WorkflowError, User?> =
    findUser { userRepository.findByMsisdn(msisdn) }

  override suspend fun findUserByEmail(email: String): Either<WorkflowError, User?> =
    findUser { userRepository.findByEmail(email) }

  private suspend fun saveNewUser(user: UserEntity): Either<WorkflowError, User> = withContextIO {
    either {
      val domainUser = user.toUser().fold(
        { raise(WorkflowValidationError(it)) },
        { it }
      )
      ensureAuthorized(domainUser.acl(), Permission.MANAGE)
      workflowBoundary {
        userRepository.save(user)
      }
        .toUser()
        .fold(
          { raise(WorkflowValidationError(it)) },
          { it }
        )
    }
  }

  override suspend fun handle(event: UserRegisteredEvent): Either<WorkflowError, UserRegisteredEvent> =
    withContextIO {
      saveNewUser(event.toEntity()).fold(
        { it.left() },
        { event.right() }
      )
    }

  override suspend fun <T : UserEvent> handle(event: T): Either<WorkflowError, T> = withContextIO {
    either {
      workflowBoundary {
        userRepository.findByUserId(event.userDto.id)
      }
        ?.let {
          ensureAuthorized(it.acl(), Permission.WRITE)
          it.handle(event)
        }
        ?.let {
          workflowBoundary { userRepository.save(it) }
        }
      event
    }
  }

  private suspend fun findUser(load: () -> UserEntity?): Either<WorkflowError, User?> =
    withContextIO {
      either {
        val entity = workflowBoundary {
          load()
        } ?: return@either null
        val user = entity.toUser().fold(
          { raise(WorkflowValidationError(it)) },
          { it }
        )
        ensureAuthorized(user.acl(), Permission.READ)
        user
      }
    }

  context(_: Raise<WorkflowError>)
  private suspend fun ensureAuthorized(
    acl: io.liquidsoftware.common.security.acl.Acl,
    permission: Permission,
  ) {
    either {
      ac.ensurePermission(acl, permission)
    }.fold(
      { raise(it.toWorkflowError()) },
      {}
    )
  }

  private fun UserEntity.toUser(): Either<ValidationErrors, User> {
    val entity = this
    return when {

      !this.active -> either {
        DisabledUser.of(
          entity.userId,
          entity.msisdn,
          entity.email,
          entity.password,
          entity.roles.firstOrNull()
            ?: raise(validationErrors("Disabled user ${entity.userId} is missing a role"))
        )
      }

      Role.ROLE_USER in this.roles -> either {
        ActiveUser.of(entity.userId, entity.msisdn, entity.email, entity.password)
      }

      Role.ROLE_ADMIN in this.roles -> either {
        AdminUser.of(entity.userId, entity.msisdn, entity.email, entity.password)
      }

      else -> {
        val err = "Unknown User Type. roles=${this.roles}"
        logger.error(err)
        validationErrors(err).left()
      }
    }
  }

  private fun RoleDto.toRole() = Role.valueOf(name)

  private fun UserRegisteredEvent.toEntity() : UserEntity {
    return UserEntity(
      userId = this.userDto.id,
      msisdn = this.userDto.msisdn,
      email = this.userDto.email,
      password = this.password,
      roles = this.userDto.roles.map { it.toRole() }.toMutableList()
    )
  }

  private fun validationErrors(message: String): ValidationErrors =
    listOf(ValidationError(message)).toNonEmptyListOrNull()!!

}
