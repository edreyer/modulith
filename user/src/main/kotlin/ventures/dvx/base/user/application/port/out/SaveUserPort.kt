package ventures.dvx.base.user.application.port.out

import ventures.dvx.base.user.domain.ActiveUser
import ventures.dvx.base.user.domain.UnregisteredUser

interface SaveUserPort {
  fun saveUser(user: UnregisteredUser): ActiveUser
}
