package io.liquidsoftware.base.server

import io.liquidsoftware.base.booking.config.BookingModuleConfig
import io.liquidsoftware.base.server.config.ServerConfig
import io.liquidsoftware.base.user.config.UserModuleConfig
import org.springframework.boot.WebApplicationType
import org.springframework.boot.actuate.autoconfigure.endpoint.jmx.JmxEndpointAutoConfiguration
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.jmx.JmxAutoConfiguration
import org.springframework.boot.builder.SpringApplicationBuilder

@SpringBootApplication(
  scanBasePackages = ["io.liquidsoftware.base.server"],
  exclude = [JmxAutoConfiguration::class, JmxEndpointAutoConfiguration::class]
)
class MicrolithApplication

fun main(args: Array<String>) {
  val parent = SpringApplicationBuilder()
    .parent(MicrolithApplication::class.java, ServerConfig::class.java)
    .web(WebApplicationType.REACTIVE)

  parent.run(*args)

  parent
    .child(UserModuleConfig::class.java)
      .properties("spring.config.name=user")
      .web(WebApplicationType.NONE)
    .sibling(BookingModuleConfig::class.java)
      .properties("spring.config.name=booking")
      .web(WebApplicationType.NONE)
    .run(*args)
}

