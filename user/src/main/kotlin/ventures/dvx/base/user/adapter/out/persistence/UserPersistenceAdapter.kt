package ventures.dvx.base.user.adapter.out.persistence

import arrow.core.Nel
import arrow.core.identity
import org.springframework.context.event.EventListener
import ventures.dvx.base.user.application.port.`in`.RoleDto
import ventures.dvx.base.user.application.port.`in`.UserEvent
import ventures.dvx.base.user.application.port.`in`.UserRegisteredEvent
import ventures.dvx.base.user.application.port.out.FindUserPort
import ventures.dvx.base.user.domain.ActiveUser
import ventures.dvx.base.user.domain.AdminUser
import ventures.dvx.base.user.domain.DisabledUser
import ventures.dvx.base.user.domain.Role
import ventures.dvx.base.user.domain.UnregisteredUser
import ventures.dvx.base.user.domain.User
import ventures.dvx.common.logging.LoggerDelegate
import ventures.dvx.common.types.ValidationError
import ventures.dvx.common.types.ValidationException

internal class UserPersistenceAdapter(
  private val userRepository: UserRepository
) : FindUserPort {

  private val logger by LoggerDelegate()

  override fun findUserById(userId: String): User? =
    userRepository.findByUserId(userId)?.toUser()

  override fun findUserByMsisdn(msisdn: String) : User? =
    userRepository.findByMsisdn(msisdn)?.toUser()

  override fun findUserByEmail(email: String) : User? =
    userRepository.findByEmail(email)?.toUser()

  private fun saveNewUser(user: UserEntity): User =
    userRepository.save(user).toUser()

  @EventListener(UserRegisteredEvent::class)
  fun handle(event: UserRegisteredEvent) {
    saveNewUser(event.toEntity())
  }

  @EventListener(UserEvent::class)
  fun handle(event: UserEvent) {
    userRepository.findByUserId(event.userDto.id)
      ?.handle(event)
      ?.let { userRepository.save(it) }
  }

  private fun UserEntity.toUser() : User {
    val errorHandler = { errors: Nel<ValidationError> ->
      val err = errors.map { it.toString() }.joinToString { "\n" }
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
      msisdn = this.userDto.msisdn,
      email = this.userDto.email,
      password = this.password,
      roles = this.userDto.roles.map { it.toRole() }.toMutableList()
    )
  }

  private fun UnregisteredUser.toEntity() : UserEntity {
    val role = when(this.role) {
      Role.ROLE_USER -> Role.ROLE_USER
      Role.ROLE_ADMIN -> Role.ROLE_ADMIN
    }
    return UserEntity(
      msisdn = this.msisdn.value,
      email = this.email.value,
      password = this.encryptedPassword.value,
      roles = mutableListOf(role)
    )
  }

}
