package io.liquidsoftware.common.config

import io.liquidsoftware.common.workflow.integration.RequestGateway
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.integration.channel.DirectChannel
import org.springframework.integration.config.EnableIntegration
import org.springframework.integration.gateway.GatewayProxyFactoryBean
import org.springframework.messaging.MessageChannel

@Configuration
@EnableIntegration
@EnableConfigurationProperties
@ComponentScan(basePackages = ["io.liquidsoftware.common"])
@ConfigurationPropertiesScan(basePackages = ["io.liquidsoftware.common"])
class WorkflowConfig {

  @Bean
  fun requestChannel(): MessageChannel = DirectChannel()

  @Bean
  fun replyChannel(): MessageChannel = DirectChannel()

  @Bean
  fun gateway(): GatewayProxyFactoryBean<RequestGateway> {
    return GatewayProxyFactoryBean(RequestGateway::class.java).apply {
      setDefaultRequestChannel(requestChannel())
      setDefaultReplyChannel(replyChannel())
    }
  }

}
