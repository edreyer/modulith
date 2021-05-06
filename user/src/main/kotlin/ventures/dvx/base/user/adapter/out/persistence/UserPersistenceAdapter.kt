package ventures.dvx.base.user.adapter.out.persistence

import arrow.core.NonEmptyList
import org.slf4j.Logger
import ventures.dvx.base.user.application.port.out.FindUserPort
import ventures.dvx.base.user.application.port.out.SaveUserPort
import ventures.dvx.base.user.domain.ActiveUser
import ventures.dvx.base.user.domain.UnregisteredUser
import ventures.dvx.base.user.domain.User
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
  val logger: Logger,
  private val userRepository: UserRepository
) : FindUserPort, SaveUserPort {

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
      val activeUsertoEntityMapper = DataClassMapper<ActiveUser, UserEntity>()
        .targetParameterSupplier("roles") { this.roles.toJpa()}
        .targetParameterSupplier("password") { this.encryptedPassword.value }
        .targetParameterSupplier("email") { this.email.value}
        .targetParameterSupplier("username") {this.username.value}
      userRepository.findByUsername(this.username.value)
        ?.copy(email = this.email.value, password = this.encryptedPassword.value, roles = this.roles.toJpa())
        ?: activeUsertoEntityMapper(this)
    }
    is UnregisteredUser -> {
      val unregisteredUsertoEntityMapper = DataClassMapper<UnregisteredUser, UserEntity>()
        .targetParameterSupplier("password") { this.encryptedPassword }
        .targetParameterSupplier("roles") { listOf(Role.ROLE_USER) }
      unregisteredUsertoEntityMapper(this)
    }
  }

  override fun findUserByUsername(username: String) : User? =
    userRepository.findByUsername(username)?.toActiveUser()

  override fun findUserByEmail(email: String) : User? =
    userRepository.findByEmail(email)?.toActiveUser()

  override fun saveUser(user: UnregisteredUser): ActiveUser =
    userRepository.save(user.toEntity()).toActiveUser()

}
