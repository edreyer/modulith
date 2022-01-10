package io.liquidsoftware.base.user.adapter.out.persistence

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
internal interface UserRepository : JpaRepository<UserEntity, String> {

  @Query(value = "from UserEntity where id = :userId")
  fun findByUserId(userId: String) : UserEntity?

  fun findByEmail(email: String) : UserEntity?

  fun findByMsisdn(msisdn: String) : UserEntity?

}
