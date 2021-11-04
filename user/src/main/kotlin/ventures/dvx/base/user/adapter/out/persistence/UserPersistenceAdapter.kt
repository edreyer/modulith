package ventures.dvx.base.user.adapter.out.persistence

import ventures.dvx.base.user.application.port.out.FindUserPort
import ventures.dvx.base.user.application.port.out.SaveNewUserPort
import ventures.dvx.base.user.domain.ActiveUser
import ventures.dvx.base.user.domain.AdminUser
import ventures.dvx.base.user.domain.DisabledUser
import ventures.dvx.base.user.domain.UnregisteredUser
import ventures.dvx.base.user.domain.User
import ventures.dvx.common.logging.LoggerDelegate

class UserPersistenceAdapter(
  private val userRepository: UserRepository
) : FindUserPort, SaveNewUserPort {

  private val logger by LoggerDelegate()

  override fun findUserByMsisdn(msisdn: String) : User? =
    userRepository.findByMsisdn(msisdn)?.toUser()

  override fun findUserByEmail(email: String) : User? =
    userRepository.findByEmail(email)?.toUser()

  override fun saveNewUser(user: UnregisteredUser): User =
    when(val newUser = userRepository.save(user.toEntity()).toUser()) {
      is ActiveUser -> newUser
      else -> {
        val err = ""
        logger.error(err)
        throw IllegalStateException(err)
    }
    }

  private fun UserEntity.toUser() : User = when {
    Role.ROLE_USER in this.roles -> ActiveUser.of(
      this.msisdn, this.email, this.password
    )
      .fold(
        { errors ->
          val err = errors.map { it.toString() }.joinToString { "\n" }
          logger.error(err)
          throw IllegalStateException(err)
        },
        { user -> user }
      )
    Role.ROLE_ADMIN in this.roles -> AdminUser.of(
      this.msisdn, this.email, this.password
    )
      .fold(
        { errors ->
          val err = errors.map { it.toString() }.joinToString { "\n" }
          logger.error(err)
          throw IllegalStateException(err)
        },
        { user -> user }
      )
    !this.active -> DisabledUser.of(
      this.msisdn, this.email, this.password
    ).fold(
      { errors ->
        val err = errors.map { it.toString() }.joinToString { "\n" }
        logger.error(err)
        throw IllegalStateException(err)
      },
      { user -> user }
    )
    else -> {
      val err = "Unknown User Type. roles=${this.roles}"
      logger.error(err)
      throw IllegalStateException(err)
    }
  }

  private fun User.toEntity() : UserEntity {
    val roles = when(this) {
      is AdminUser -> listOf(Role.ROLE_ADMIN)
      else -> listOf(Role.ROLE_USER)
    }
    val that = this
    return userRepository.findByEmail(that.email.value)
      ?: userRepository.findByMsisdn(that.msisdn.value)
      ?.apply {
        this.msisdn = that.msisdn.value
        this.email = that.email.value
        this.active = when (that) {
          is UnregisteredUser -> true
          is ActiveUser -> true
          is AdminUser -> true
          is DisabledUser -> false
        }
      }
      ?: UserEntity(
        msisdn = this.msisdn.value,
        email = this.email.value,
        password = this.encryptedPassword.value,
        roles = when (that) {
          is UnregisteredUser -> mutableListOf(Role.ROLE_USER)
          is ActiveUser -> mutableListOf(Role.ROLE_USER)
          is AdminUser -> mutableListOf(Role.ROLE_ADMIN)
          is DisabledUser -> mutableListOf()
        }
      )
  }

}
