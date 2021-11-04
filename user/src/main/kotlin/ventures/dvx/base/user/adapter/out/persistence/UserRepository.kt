package ventures.dvx.base.user.adapter.out.persistence

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface UserRepository : JpaRepository<UserEntity, String> {

  fun findByEmail(email: String) : UserEntity?

  fun findByMsisdn(msisdn: String) : UserEntity?

}
