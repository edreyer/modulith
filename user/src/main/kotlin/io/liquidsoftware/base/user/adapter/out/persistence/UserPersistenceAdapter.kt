package io.liquidsoftware.base.user.adapter.out.persistence

import arrow.core.identity
import arrow.core.raise.effect
import arrow.core.raise.fold
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
import io.liquidsoftware.common.errors.ErrorHandling.ERROR_HANDLER
import io.liquidsoftware.common.ext.withContextIO
import io.liquidsoftware.common.logging.LoggerDelegate
import io.liquidsoftware.common.security.acl.AclChecker
import io.liquidsoftware.common.security.acl.Permission

internal class UserPersistenceAdapter(
  private val userRepository: UserRepository,
  private val ac: AclChecker
) : FindUserPort, UserEventPort {

  private val logger by LoggerDelegate()

  override suspend fun findUserById(userId: String): User? = withContextIO {
    kotlin.runCatching {
      userRepository.findByUserId(userId)
        ?.toUser()
        ?.also { ac.checkPermission(it.acl(), Permission.READ)}
    }.getOrNull()
  }

  override suspend fun findUserByMsisdn(msisdn: String) : User? = withContextIO {
    runCatching {
      userRepository.findByMsisdn(msisdn)
        ?.toUser()
        ?.also { ac.checkPermission(it.acl(), Permission.READ)}
    }.getOrNull()
  }

  override suspend fun findUserByEmail(email: String) : User? = withContextIO {
    runCatching {
      userRepository.findByEmail(email)
        ?.toUser()
        ?.also { ac.checkPermission(it.acl(), Permission.READ)}
    }
      .getOrNull()
  }

  private suspend fun saveNewUser(user: UserEntity): User = withContextIO {
    ac.checkPermission(user.toUser().acl(), Permission.MANAGE)
    userRepository.save(user)
      .toUser()
  }

  override suspend fun handle(event: UserRegisteredEvent): UserRegisteredEvent = withContextIO {
    saveNewUser(event.toEntity())
    event
  }

  override suspend fun <T : UserEvent> handle(event: T): T = withContextIO {
    userRepository.findByUserId(event.userDto.id)
      ?.also { ac.checkPermission(it.acl(), Permission.WRITE) }
      ?.handle(event)
      ?.let { userRepository.save(it) }
    event
  }

  private suspend fun UserEntity.toUser() : User {
    val entity = this
    return when {

      !this.active -> effect {
        DisabledUser.of(entity.userId, entity.msisdn, entity.email, entity.password, entity.roles.first())
      }.fold(ERROR_HANDLER, ::identity)

      Role.ROLE_USER in this.roles -> effect {
        ActiveUser.of(entity.userId, entity.msisdn, entity.email, entity.password)
      }.fold(ERROR_HANDLER, ::identity)

      Role.ROLE_ADMIN in this.roles -> effect {
        AdminUser.of(entity.userId, entity.msisdn, entity.email, entity.password)
      }.fold(ERROR_HANDLER, ::identity)

      else -> {
        val err = "Unknown User Type. roles=${this.roles}"
        logger.error(err)
        throw IllegalStateException(err)
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

}
