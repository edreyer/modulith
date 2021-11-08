package ventures.dvx.base.user.application.port.out

import ventures.dvx.base.user.domain.User

interface FindUserPort {
  fun findUserById(userId: String): User?
  fun findUserByEmail(email: String): User?
  fun findUserByMsisdn(msisdn: String): User?
}
