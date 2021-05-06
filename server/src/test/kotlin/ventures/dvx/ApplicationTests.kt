package ventures.dvx.ventures.dvx

import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import ventures.dvx.DvxApplication

@SpringBootTest(
  classes = [DvxApplication::class],
  webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
class ApplicationTests {

  @Test
  fun contextLoads() {}

}
