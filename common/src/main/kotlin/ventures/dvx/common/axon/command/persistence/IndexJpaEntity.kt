package ventures.dvx.common.axon.command.persistence

import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.Table

@Entity
@Table(name = "index_entity")
data class IndexJpaEntity(
  @Id
  val id: String = IdGenerator.newId(),
  val aggregateName: String,
  val key: String
)
