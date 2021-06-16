package ventures.dvx.common.axon.command.persistence

import java.util.*
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.Table

@Entity
@Table(name = "index_entity")
data class IndexJpaEntity(
  @Id
  val aggregateId: UUID,
  val aggregateName: String,
  val key: String
)
