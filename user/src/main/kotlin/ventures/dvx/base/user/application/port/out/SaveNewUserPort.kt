package ventures.dvx.base.user.application.port.out

import ventures.dvx.base.user.domain.UnregisteredUser
import ventures.dvx.base.user.domain.User

interface SaveNewUserPort {
  fun saveNewUser(user: UnregisteredUser): User
}
