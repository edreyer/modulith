package io.liquidsoftware.base

import assertk.assertThat
import assertk.assertions.isNotNull
import io.liquidsoftware.base.booking.application.port.`in`.AppointmentApi
import io.liquidsoftware.base.payment.application.port.`in`.PaymentApi
import io.liquidsoftware.base.server.ModulithApplication
import io.liquidsoftware.base.server.config.ServerConfig
import io.liquidsoftware.base.user.application.port.`in`.SystemFindUserByEmailApi
import io.liquidsoftware.common.context.ModuleApiRegistry
import org.junit.jupiter.api.Test
import org.springframework.boot.WebApplicationType
import org.springframework.boot.builder.SpringApplicationBuilder
import org.springframework.context.ConfigurableApplicationContext

class ModulithHierarchyBootTest {

  @Test
  fun `real modular hierarchy boots with local api proxies`() {
    var parent: ConfigurableApplicationContext? = null

    try {
      val builder = SpringApplicationBuilder()
        .parent(ModulithApplication::class.java, ServerConfig::class.java)
        .properties("server.port=0")
        .web(WebApplicationType.SERVLET)

      parent = builder.run()
      builder
        .child(io.liquidsoftware.base.user.config.UserModuleConfig::class.java)
          .properties("spring.config.name=user")
          .web(WebApplicationType.NONE)
        .sibling(io.liquidsoftware.base.booking.config.BookingModuleConfig::class.java)
          .properties("spring.config.name=booking")
          .web(WebApplicationType.NONE)
        .sibling(io.liquidsoftware.base.payment.config.PaymentModuleConfig::class.java)
          .properties("spring.config.name=payment")
          .web(WebApplicationType.NONE)
        .run()

      assertThat(parent.getBean(SystemFindUserByEmailApi::class.java)).isNotNull()
      assertThat(parent.getBean(org.springframework.security.core.userdetails.UserDetailsService::class.java)).isNotNull()
      assertThat(ModuleApiRegistry.require(PaymentApi::class)).isNotNull()
      assertThat(ModuleApiRegistry.require(AppointmentApi::class)).isNotNull()
    } finally {
      parent?.close()
    }
  }
}
