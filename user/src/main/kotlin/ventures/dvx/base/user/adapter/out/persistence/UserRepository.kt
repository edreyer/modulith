package ventures.dvx.base.user.adapter.out.persistence

import ventures.dvx.base.user.application.port.out.FindUserPort

interface UserRepository {
  fun save(user: UserEntity): UserEntity
  fun findByUsername(username: String): UserEntity?
  fun findByEmail(email: String): UserEntity?
}
