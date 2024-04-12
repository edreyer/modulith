package io.liquidsoftware.base.user.adapter.out.persistence

import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository

@Repository
internal interface UserRepository : MongoRepository<UserEntity, String> {

  fun findByUserId(userId: String) : UserEntity?

  fun findByEmail(email: String) : UserEntity?

  fun findByMsisdn(msisdn: String) : UserEntity?

}
