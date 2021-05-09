package ventures.dvx.user.arch

import com.tngtech.archunit.core.importer.ClassFileImporter
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses
import org.junit.jupiter.api.Test
import ventures.dvx.arch.HexagonalArchitecture


class DependencyRuleTests {

  @Test
  fun validateRegistrationContextArchitecture() {
    HexagonalArchitecture.boundedContext("ventures.dvx.base.user")

      .withDomainLayer("domain")

      .withAdaptersLayer("adapter")
      .incoming("in.web")
      .outgoing("out.persistence")
      .and()

      .withApplicationLayer("application")
      .useCases("usecase")
      .incomingPorts("port.in")
      .outgoingPorts("port.out")
      .and()

      .withConfiguration("config")
      .check(
        ClassFileImporter()
          .importPackages("ventures.dvx.base.user..")
      )
  }

  @Test
  fun testDomainPackageDependencies() {
    noClasses()
      .that()
      .resideInAPackage("ventures.dvx.base.user.domain..")
      .should()
      .dependOnClassesThat()
      .resideInAnyPackage("ventures.dvx.base.user.application..")
      .check(
        ClassFileImporter()
          .importPackages("ventures.dvx.base.user..")
      )
  }

  @Test
  fun testUseCasePackageDependencies() {
    noClasses()
      .that()
      .resideInAPackage("ventures.dvx.base.user.application.usecase..")
      .should()
      .dependOnClassesThat()
      .resideInAnyPackage("ventures.dvx.base.user.adapter..")
      .check(
        ClassFileImporter()
          .importPackages("ventures.dvx.base.user..")
      )
  }

}
