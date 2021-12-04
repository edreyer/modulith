package io.liquidsoftware.base.booking.arch

import com.tngtech.archunit.core.importer.ClassFileImporter
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses
import io.liquidsoftware.arch.HexagonalArchitecture
import org.junit.jupiter.api.Test

class DependencyRuleTests {

  val module = "booking"

  @Test
  fun validateBookingContextArchitecture() {
    HexagonalArchitecture.boundedContext("io.liquidsoftware.base.$module")

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
          .importPackages("io.liquidsoftware.base.$module..")
      )
  }

  @Test
  fun testDomainPackageDependencies() {
    noClasses()
      .that()
      .resideInAPackage("io.liquidsoftware.base.$module.domain..")
      .should()
      .dependOnClassesThat()
      .resideInAnyPackage("io.liquidsoftware.base.$module.application..")
      .check(
        ClassFileImporter()
          .importPackages("io.liquidsoftware.base.$module..")
      )

    noClasses()
      .that()
      .resideInAPackage("io.liquidsoftware.base.$module.domain..")
      .should()
      .dependOnClassesThat()
      .resideInAnyPackage("io.liquidsoftware.base.$module.adapter..")
      .check(
        ClassFileImporter()
          .importPackages("io.liquidsoftware.base.$module..")
      )

  }

  @Test
  fun testWorkflowPackageDependencies() {
    noClasses()
      .that()
      .resideInAPackage("io.liquidsoftware.base.$module.application.workflows..")
      .should()
      .dependOnClassesThat()
      .resideInAnyPackage("io.liquidsoftware.base.$module.adapter..")
      .check(
        ClassFileImporter()
          .importPackages("io.liquidsoftware.base.$module..")
      )
  }

}
