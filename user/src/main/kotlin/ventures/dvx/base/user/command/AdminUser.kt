package ventures.dvx.base.user.command

import org.axonframework.modelling.command.AggregateIdentifier
import ventures.dvx.common.axon.IndexableAggregate
import java.util.*

class AdminUser: IndexableAggregate() {

  @AggregateIdentifier
  private lateinit var id: UUID

  private lateinit var username: String

  override val businessKey: String
    get() = username
}
