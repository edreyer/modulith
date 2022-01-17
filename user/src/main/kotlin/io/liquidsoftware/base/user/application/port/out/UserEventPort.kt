package io.liquidsoftware.base.user.application.port.out

import io.liquidsoftware.base.user.application.port.`in`.UserEvent
import io.liquidsoftware.base.user.application.port.`in`.UserRegisteredEvent

interface UserEventPort {

  suspend fun handle(event: UserRegisteredEvent): UserRegisteredEvent
  suspend fun <T: UserEvent> handle(event: T): T

}
