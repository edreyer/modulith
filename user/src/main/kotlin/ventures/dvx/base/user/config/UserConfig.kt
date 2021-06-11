package ventures.dvx.base.user.config

import org.axonframework.common.caching.Cache
import org.axonframework.common.caching.WeakReferenceCache
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class UserConfig {

  @Bean
  fun userCache(): Cache {
    return WeakReferenceCache()
  }

}
