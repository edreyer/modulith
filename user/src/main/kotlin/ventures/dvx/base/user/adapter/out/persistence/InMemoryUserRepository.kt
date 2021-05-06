package ventures.dvx.base.user.adapter.out.persistence

/** Temporary class until we move to Spring Data Jdbc */
class InMemoryUserRepository : UserRepository {

  private val usersByEmail: MutableMap<String, UserEntity> = mutableMapOf();

  override fun findByUsername(username: String): UserEntity? =
    usersByEmail.values.firstOrNull() { it.username == username }

  override fun findByEmail(email: String): UserEntity? = usersByEmail[email]

  override fun save(user: UserEntity): UserEntity {
    usersByEmail[user.email] = user
    return user
  }

}

