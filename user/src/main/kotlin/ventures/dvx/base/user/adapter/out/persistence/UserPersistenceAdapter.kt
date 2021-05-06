package ventures.dvx.base.user.adapter.out.persistence

import org.slf4j.Logger
import org.springframework.stereotype.Component
import ventures.dvx.base.user.application.port.out.FindUserPort
import ventures.dvx.base.user.application.port.out.SaveUserPort
import ventures.dvx.base.user.domain.ActiveUser
import ventures.dvx.base.user.domain.UnregisteredUser
import ventures.dvx.base.user.domain.User

@Component
class UserPersistenceAdapter(
  val logger: Logger,
  private val userRepository: UserRepository
) : FindUserPort, SaveUserPort {

  private fun UserEntity.toActiveUser() : ActiveUser = ActiveUser.of(
    this.username, this.email, this.password)
    .fold(
      { errors ->
        val errorMsg = errors.map { it.toString() }.joinToString { "\n" }
        logger.error(errorMsg)
        throw IllegalStateException(errorMsg)
      },
      { activeUser -> activeUser }
    )

  private fun User.toEntity() : UserEntity = when (this) {
    is ActiveUser -> userRepository.findByUsername(this.username.value)
      ?.copy(email = this.email.value, password = this.encryptedPassword.value)
      ?: UserEntity(this.username.value, this.email.value, this.encryptedPassword.value, this.roles.toList())
    is UnregisteredUser ->
      UserEntity(this.username, this.email, this.encryptedPassword, listOf(Role.ROLE_USER))
  }

  override fun findUserByUsername(username: String) : User? =
    userRepository.findByUsername(username)?.toActiveUser()

  override fun findUserByEmail(email: String) : User? =
    userRepository.findByEmail(email)?.toActiveUser()

  override fun saveUser(user: UnregisteredUser): ActiveUser =
    userRepository.save(user.toEntity()).toActiveUser()

}
