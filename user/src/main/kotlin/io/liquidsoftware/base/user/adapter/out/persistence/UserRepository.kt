package io.liquidsoftware.base.user.adapter.out.persistence

import org.springframework.data.mongodb.repository.ReactiveMongoRepository
import org.springframework.stereotype.Repository
import reactor.core.publisher.Mono

@Repository
internal interface UserRepository : ReactiveMongoRepository<UserEntity, String> {

  fun findByUserId(userId: String) : Mono<UserEntity>

  fun findByEmail(email: String) : Mono<UserEntity>

  fun findByMsisdn(msisdn: String) : Mono<UserEntity>

}
