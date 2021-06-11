package ventures.dvx.common.axon.command.persistence

import java.util.*

object IdGenerator {
  fun newId() = UUID.randomUUID().toString().replace("-", "")
}
