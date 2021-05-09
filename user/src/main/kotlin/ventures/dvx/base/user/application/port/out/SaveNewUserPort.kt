package ventures.dvx.base.user.application.port.out

import ventures.dvx.base.user.domain.ActiveUser
import ventures.dvx.base.user.domain.UnregisteredUser

interface SaveNewUserPort {
  fun saveNewUser(user: UnregisteredUser): ActiveUser
}
