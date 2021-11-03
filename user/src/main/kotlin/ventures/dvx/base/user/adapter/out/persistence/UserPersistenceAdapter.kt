package ventures.dvx.base.user.adapter.out.persistence

import ventures.dvx.base.user.application.port.out.FindUserPort
import ventures.dvx.base.user.application.port.out.SaveNewUserPort
import ventures.dvx.base.user.domain.ActiveUser
import ventures.dvx.base.user.domain.AdminUser
import ventures.dvx.base.user.domain.UnregisteredUser
import ventures.dvx.base.user.domain.User
import ventures.dvx.common.logging.LoggerDelegate

class UserPersistenceAdapter(
  private val userRepository: UserRepository
) : FindUserPort, SaveNewUserPort {

  private val logger by LoggerDelegate()

  override fun findUserByUsername(username: String) : User? =
    userRepository.findByUsername(username)?.toUser()

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
      this.username, this.email, this.password
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
      this.username, this.email, this.password
    )
      .fold(
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
    return userRepository.findByUsername(this.username.value)
      ?.copy(email = this.email.value, password = this.encryptedPassword.value)
      ?: UserEntity(
        email = this.email.value,
        username = this.username.value,
        password = this.encryptedPassword.value,
        roles = roles
      )
  }

}
