package io.liquidsoftware.base.user.config

import io.liquidsoftware.base.user.adapter.out.module.LocalFindUserApi
import io.liquidsoftware.base.user.adapter.out.module.LocalRegisterUserApi
import io.liquidsoftware.base.user.adapter.out.module.LocalSystemFindUserByEmailApi
import io.liquidsoftware.base.user.adapter.out.module.LocalUserAdminApi
import io.liquidsoftware.base.user.application.port.`in`.FindUserApi
import io.liquidsoftware.base.user.application.port.`in`.RegisterUserApi
import io.liquidsoftware.base.user.application.port.`in`.SystemFindUserByEmailApi
import io.liquidsoftware.base.user.application.port.`in`.UserAdminApi
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class UserModuleAdapterConfig {
  @Bean fun registerUserApi(): RegisterUserApi = LocalRegisterUserApi()

  @Bean fun findUserApi(): FindUserApi = LocalFindUserApi()

  @Bean fun userAdminApi(): UserAdminApi = LocalUserAdminApi()

  @Bean fun systemFindUserByEmailApi(): SystemFindUserByEmailApi = LocalSystemFindUserByEmailApi()
}
