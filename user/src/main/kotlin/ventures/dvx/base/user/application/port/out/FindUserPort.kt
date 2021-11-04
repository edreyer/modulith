package ventures.dvx.base.user.application.port.out

import ventures.dvx.base.user.domain.User

interface FindUserPort {
  fun findUserByMsisdn(msisdn: String): User?
  fun findUserByEmail(email: String): User?
}
