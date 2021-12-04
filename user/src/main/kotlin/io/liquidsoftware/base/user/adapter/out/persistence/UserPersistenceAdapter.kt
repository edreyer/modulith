package io.liquidsoftware.base.user.adapter.out.persistence

import arrow.core.Nel
import arrow.core.identity
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
import io.liquidsoftware.common.logging.LoggerDelegate
import io.liquidsoftware.common.security.acl.AclChecker
import io.liquidsoftware.common.security.acl.Permission
import io.liquidsoftware.common.types.ValidationError
import io.liquidsoftware.common.types.ValidationException
import io.liquidsoftware.common.types.toErrString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal class UserPersistenceAdapter(
  private val userRepository: UserRepository,
  private val ac: AclChecker
) : FindUserPort, UserEventPort {

  private val logger by LoggerDelegate()

  override suspend fun findUserById(userId: String): User? = withContext(Dispatchers.IO) {
    userRepository.findByUserId(userId)
      ?.toUser()
      ?.also { ac.checkPermission(it.acl(), Permission.READ)}
  }

  override suspend fun findUserByMsisdn(msisdn: String) : User? = withContext(Dispatchers.IO) {
    userRepository.findByMsisdn(msisdn)
      ?.toUser()
      ?.also { ac.checkPermission(it.acl(), Permission.READ)}
  }

  override suspend fun findUserByEmail(email: String) : User? = withContext(Dispatchers.IO) {
    userRepository.findByEmail(email)
      ?.toUser()
      ?.also { ac.checkPermission(it.acl(), Permission.READ)}
  }

  private suspend fun saveNewUser(user: UserEntity): User = withContext(Dispatchers.IO) {
    ac.checkPermission(user.toUser().acl(), Permission.MANAGE)
    userRepository.saveAndFlush(user).toUser()
  }

  override suspend fun handle(event: UserRegisteredEvent): UserRegisteredEvent = withContext(Dispatchers.IO) {
    saveNewUser(event.toEntity())
    event
  }

  override suspend fun <T : UserEvent> handle(event: T): T = withContext(Dispatchers.IO) {
    userRepository.findByUserId(event.userDto.id)
      ?.also { ac.checkPermission(it.acl(), Permission.WRITE) }
      ?.handle(event)
      ?.let { userRepository.saveAndFlush(it) }
    event
  }

  private fun UserEntity.toUser() : User {
    val errorHandler = { errors: Nel<ValidationError> ->
      val err = errors.toErrString()
      logger.error(err)
      throw ValidationException(errors)
    }
    return when {
      !this.active -> DisabledUser.of(this.id, this.msisdn, this.email, this.password, this.roles.first())
        .fold(errorHandler, ::identity)
      Role.ROLE_USER in this.roles -> ActiveUser.of(this.id, this.msisdn, this.email, this.password)
        .fold(errorHandler, ::identity)
      Role.ROLE_ADMIN in this.roles -> AdminUser.of(this.id, this.msisdn, this.email, this.password)
        .fold(errorHandler, ::identity)
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
