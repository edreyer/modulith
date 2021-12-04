package ventures.dvx.base.booking.arch

import com.tngtech.archunit.core.importer.ClassFileImporter
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses
import org.junit.jupiter.api.Test
import ventures.dvx.arch.HexagonalArchitecture


class DependencyRuleTests {

  val module = "booking"

  @Test
  fun validateBookingContextArchitecture() {
    HexagonalArchitecture.boundedContext("ventures.dvx.base.$module")

      .withDomainLayer("domain")

      .withAdaptersLayer("adapter")
      .incoming("in.web")
      .outgoing("out.persistence")
      .and()

      .withApplicationLayer("application")
      .workflows("workflows")
      .incomingPorts("port.in")
      .outgoingPorts("port.out")
      .and()
      .withConfiguration("config")
      .check(
        ClassFileImporter()
          .importPackages("ventures.dvx.base.$module..")
      )
  }

  @Test
  fun testDomainPackageDependencies() {
    noClasses()
      .that()
      .resideInAPackage("ventures.dvx.base.$module.domain..")
      .should()
      .dependOnClassesThat()
      .resideInAnyPackage("ventures.dvx.base.$module.application..")
      .check(
        ClassFileImporter()
          .importPackages("ventures.dvx.base.$module..")
      )

    noClasses()
      .that()
      .resideInAPackage("ventures.dvx.base.$module.domain..")
      .should()
      .dependOnClassesThat()
      .resideInAnyPackage("ventures.dvx.base.$module.adapter..")
      .check(
        ClassFileImporter()
          .importPackages("ventures.dvx.base.$module..")
      )

  }

  @Test
  fun testWorkflowPackageDependencies() {
    noClasses()
      .that()
      .resideInAPackage("ventures.dvx.base.$module.application.workflows..")
      .should()
      .dependOnClassesThat()
      .resideInAnyPackage("ventures.dvx.base.$module.adapter..")
      .check(
        ClassFileImporter()
          .importPackages("ventures.dvx.base.$module..")
      )
  }

}
