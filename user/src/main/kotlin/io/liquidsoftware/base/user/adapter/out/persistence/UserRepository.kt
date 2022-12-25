package io.liquidsoftware.base.user.adapter.out.persistence

import org.springframework.data.mongodb.repository.ReactiveMongoRepository
import org.springframework.stereotype.Repository
import reactor.core.publisher.Mono

@Repository
internal interface UserRepository : ReactiveMongoRepository<UserEntity, String> {

  suspend fun findByUserId(userId: String) : Mono<UserEntity>

  suspend fun findByEmail(email: String) : Mono<UserEntity>

  suspend fun findByMsisdn(msisdn: String) : Mono<UserEntity>

}
