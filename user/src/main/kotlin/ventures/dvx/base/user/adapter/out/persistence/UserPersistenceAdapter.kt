package ventures.dvx.base.user.adapter.out.persistence

import ventures.dvx.base.user.application.port.out.FindUserPort
import ventures.dvx.base.user.application.port.out.SaveNewUserPort
import ventures.dvx.base.user.domain.ActiveUser
import ventures.dvx.base.user.domain.AdminUser
import ventures.dvx.base.user.domain.DisabledUser
import ventures.dvx.base.user.domain.UnregisteredUser
import ventures.dvx.base.user.domain.User
import ventures.dvx.common.logging.LoggerDelegate
import ventures.dvx.common.types.ValidationException

class UserPersistenceAdapter(
  private val userRepository: UserRepository
) : FindUserPort, SaveNewUserPort {

  private val logger by LoggerDelegate()

  override fun findUserById(userId: String): User? =
    userRepository.findByUserId(userId)?.toUser()

  override fun findUserByMsisdn(msisdn: String) : User? =
    userRepository.findByMsisdn(msisdn)?.toUser()

  override fun findUserByEmail(email: String) : User? =
    userRepository.findByEmail(email)?.toUser()

  override fun saveNewUser(user: UnregisteredUser): User =
    userRepository.save(user.toEntity()).toUser()

  private fun UserEntity.toUser() : User = when {
    Role.ROLE_USER in this.roles -> ActiveUser.of(this.id, this.msisdn, this.email, this.password)
      .fold(
        { errors ->
          val err = errors.map { it.toString() }.joinToString { "\n" }
          logger.error(err)
          throw ValidationException(errors)
        },
        { user -> user }
      )
    Role.ROLE_ADMIN in this.roles -> AdminUser.of(this.id, this.msisdn, this.email, this.password)
      .fold(
        { errors ->
          val err = errors.map { it.toString() }.joinToString { "\n" }
          logger.error(err)
          throw ValidationException(errors)
        },
        { user -> user }
      )
    !this.active -> DisabledUser.of(this.id, this.msisdn, this.email, this.password, this.roles.first().name
    ).fold(
      { errors ->
        val err = errors.map { it.toString() }.joinToString { "\n" }
        logger.error(err)
        throw ValidationException(errors)
      },
      { user -> user }
    )
    else -> {
      val err = "Unknown User Type. roles=${this.roles}"
      logger.error(err)
      throw IllegalStateException(err)
    }
  }

  private fun UnregisteredUser.toEntity() : UserEntity {
    val role = when(this.role.value) {
      Role.ROLE_USER.name -> Role.ROLE_USER
      Role.ROLE_ADMIN.name -> Role.ROLE_ADMIN
      else -> throw IllegalStateException("Missing a Role definition: ${this.role.value}")
    }
    return UserEntity(
      msisdn = this.msisdn.value,
      email = this.email.value,
      password = this.encryptedPassword.value,
      roles = mutableListOf(role)
    )
  }

  private fun User.toEntity() : UserEntity {
    val that = this
    return userRepository.findByUserId(that.id.value)
      ?.apply {
        this.msisdn = that.msisdn.value
        this.email = that.email.value
        this.password = that.encryptedPassword.value
        this.active = when (that) {
          is ActiveUser -> true
          is AdminUser -> true
          is DisabledUser -> false
        }
      }
      ?: throw IllegalStateException("Cannot save unsaved User instance")
  }

}
