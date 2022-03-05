package io.liquidsoftware.base

import io.liquidsoftware.base.server.ModulithApplication
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest(
  classes = [ModulithApplication::class],
  webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
class ApplicationTests {

  @Test
  fun contextLoads() {}

}
