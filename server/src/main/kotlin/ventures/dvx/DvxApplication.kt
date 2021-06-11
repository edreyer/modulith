package ventures.dvx

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication

@SpringBootApplication(
  scanBasePackages = ["ventures.dvx"]
)
@ConfigurationPropertiesScan(
  basePackages = ["ventures.dvx"]
)
class DvxApplication

fun main(args: Array<String>) {
  runApplication<DvxApplication>(*args)
}

