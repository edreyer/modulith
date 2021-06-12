package ventures.dvx.base.user.command

import org.axonframework.modelling.command.AggregateIdentifier
import java.util.*

class AdminUser: User() {

  @AggregateIdentifier
  private lateinit var id: UUID

  private lateinit var username: String

  override val businessKey: String
    get() = username
}
