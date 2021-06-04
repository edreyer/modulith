package ventures.dvx.base.user.command.persistence

import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository

@Repository
interface IndexRepository : CrudRepository<IndexEntity, Int> {

  fun findEntityByAggregateNameAndKey(aggregateName: String, key: String) : IndexEntity?

}
