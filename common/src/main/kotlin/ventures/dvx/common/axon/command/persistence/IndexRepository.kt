package ventures.dvx.common.axon.command.persistence

import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface IndexRepository : CrudRepository<IndexJpaEntity, UUID> {

  fun findEntityByAggregateNameAndKey(aggregateName: String, key: String) : IndexJpaEntity?

}
