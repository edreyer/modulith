package ventures.dvx.base.user.adapter.out.persistence

interface UserRepository {
  fun save(user: UserEntity): UserEntity
  fun findByUsername(username: String): UserEntity?
  fun findByEmail(email: String): UserEntity?
}
