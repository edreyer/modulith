package ventures.dvx.base.user.command.persistence

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import java.util.*

@Table("index_entity")
data class IndexEntity(
  @Id val id: UUID,
  val aggregateName: String,
  val key: String
)
