package ventures.dvx.base.user.adapter.out.persistence

import arrow.core.NonEmptyList
import ventures.dvx.base.user.application.port.out.SaveNewUserPort
import ventures.dvx.base.user.application.port.out.FindUserPort
import ventures.dvx.base.user.domain.ActiveUser
import ventures.dvx.base.user.domain.UnregisteredUser
import ventures.dvx.base.user.domain.User
import ventures.dvx.common.logging.LoggerDelegate
import ventures.dvx.common.mapping.DataClassMapper

fun Role.toDomain() = ventures.dvx.base.user.domain.Role.valueOf(this.toString())

fun ventures.dvx.base.user.domain.Role.toJpa() = Role.valueOf(this.toString())

fun NonEmptyList<ventures.dvx.base.user.domain.Role>.toJpa() = this.map {
  it.toJpa()
}.toList()

fun List<Role>.toDomain() = NonEmptyList.fromListUnsafe(this.map {
  it.toDomain()
})

class UserPersistenceAdapter(
  private val userRepository: UserRepository
) : FindUserPort, SaveNewUserPort {

  private val logger by LoggerDelegate()

  override fun findUserByUsername(username: String) : User? =
    userRepository.findByUsername(username)?.toActiveUser()

  override fun findUserByEmail(email: String) : User? =
    userRepository.findByEmail(email)?.toActiveUser()

  override fun saveNewUser(user: UnregisteredUser): ActiveUser =
    userRepository.save(user.toEntity()).toActiveUser()

  private fun UserEntity.toActiveUser() : ActiveUser = ActiveUser.of(
    this.username, this.email, this.password, this.roles.toDomain())
    .fold(
      { errors ->
        val errorMsg = errors.map { it.toString() }.joinToString { "\n" }
        logger.error(errorMsg)
        throw IllegalStateException(errorMsg)
      },
      { activeUser -> activeUser }
    )

  private fun User.toEntity() : UserEntity = when (this) {
    is ActiveUser -> {
      val activeUserToEntityMapper = DataClassMapper<ActiveUser, UserEntity>()
        .targetParameterSupplier(UserEntity::roles) { this.roles.toJpa()}
        .targetParameterSupplier(UserEntity::password) { this.encryptedPassword.value }
        .targetParameterSupplier(UserEntity::email) { this.email.value}
        .targetParameterSupplier(UserEntity::username) {this.username.value}
        .targetParameterSupplier(UserEntity::active) { true }

      userRepository.findByUsername(this.username.value)
        ?.copy(email = this.email.value, password = this.encryptedPassword.value, roles = this.roles.toJpa())
        ?: activeUserToEntityMapper(this)
    }
    is UnregisteredUser -> {
      val role = Role.ROLE_USER
      val unregisteredUserToEntityMapper = DataClassMapper<UnregisteredUser, UserEntity>()
        .targetParameterSupplier(UserEntity::username) { this.username.value }
        .targetParameterSupplier(UserEntity::email) { this.email.value }
        .targetParameterSupplier(UserEntity::password) { this.encryptedPassword.value }
        .targetParameterSupplier(UserEntity::roles) { listOf(role) }
        .targetParameterSupplier(UserEntity::active) { true }

      unregisteredUserToEntityMapper(this)
    }
  }

}
