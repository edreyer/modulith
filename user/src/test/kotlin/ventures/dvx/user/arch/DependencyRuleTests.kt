package io.liquidsoftware.user.arch

import com.tngtech.archunit.core.importer.ClassFileImporter
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses
import org.junit.jupiter.api.Test
import io.liquidsoftware.arch.HexagonalArchitecture


class DependencyRuleTests {

  @Test
  fun validateUserContextArchitecture() {
    HexagonalArchitecture.boundedContext("io.liquidsoftware.base.user")

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
          .importPackages("io.liquidsoftware.base.user..")
      )
  }

  @Test
  fun testDomainPackageDependencies() {
    noClasses()
      .that()
      .resideInAPackage("io.liquidsoftware.base.user.domain..")
      .should()
      .dependOnClassesThat()
      .resideInAnyPackage("io.liquidsoftware.base.user.application..")
      .check(
        ClassFileImporter()
          .importPackages("io.liquidsoftware.base.user..")
      )

    noClasses()
      .that()
      .resideInAPackage("io.liquidsoftware.base.user.domain..")
      .should()
      .dependOnClassesThat()
      .resideInAnyPackage("io.liquidsoftware.base.user.adapter..")
      .check(
        ClassFileImporter()
          .importPackages("io.liquidsoftware.base.user..")
      )

  }

  @Test
  fun testWorkflowPackageDependencies() {
    noClasses()
      .that()
      .resideInAPackage("io.liquidsoftware.base.user.application.workflows..")
      .should()
      .dependOnClassesThat()
      .resideInAnyPackage("io.liquidsoftware.base.user.adapter..")
      .check(
        ClassFileImporter()
          .importPackages("io.liquidsoftware.base.user..")
      )
  }

}
