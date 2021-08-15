package ventures.dvx.common.axon.command.persistence

import ventures.dvx.common.jpa.AbstractJpaPersistable
import java.util.*
import javax.persistence.Entity
import javax.persistence.Table

@Entity
@Table(name = "index_entity")
class IndexJpaEntity(
  @Transient private val aggregateId: UUID,
  val aggregateName: String,
  val key: String
) : AbstractJpaPersistable<UUID>(aggregateId)
