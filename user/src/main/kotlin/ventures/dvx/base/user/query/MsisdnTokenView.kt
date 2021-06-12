package ventures.dvx.base.user.query

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.time.Instant
import java.util.*
import javax.persistence.Entity
import javax.persistence.Id

@Entity
data class MsisdnTokenView(
  @Id
  var id: UUID,
  var token: String,
  var msisdn: String,
  var email: String,
  var expires: Instant,
  var used: Boolean = false
) {
  fun isTokenValid(): Boolean = !used && expires.isAfter(Instant.now())
}

@Repository
interface MsisdnTokenViewRepository: JpaRepository<MsisdnTokenView, UUID> {
  fun findByMsisdnAndTokenAndUsedFalse(msisdn: String, token: String): List<MsisdnTokenView>
}
