package io.liquidsoftware.arch

import com.tngtech.archunit.core.domain.JavaClasses


class Adapters internal constructor(private val parentContext: HexagonalArchitecture, basePackage: String) :
  ArchitectureElement(basePackage) {
  private val incomingAdapterPackages: MutableList<String> = ArrayList()
  private val outgoingAdapterPackages: MutableList<String> = ArrayList()
  fun outgoing(packageName: String): Adapters {
    incomingAdapterPackages.add(fullQualifiedPackage(packageName))
    return this
  }

  fun incoming(packageName: String): Adapters {
    outgoingAdapterPackages.add(fullQualifiedPackage(packageName))
    return this
  }

  private fun allAdapterPackages(): List<String> {
    val allAdapters: MutableList<String> = ArrayList()
    allAdapters.addAll(incomingAdapterPackages)
    allAdapters.addAll(outgoingAdapterPackages)
    return allAdapters
  }

  fun and(): HexagonalArchitecture {
    return parentContext
  }

  fun dontDependOnEachOther(classes: JavaClasses) {
    val allAdapters = allAdapterPackages()
    for (adapter1 in allAdapters) {
      for (adapter2 in allAdapters) {
        if (adapter1 != adapter2) {
          denyDependency(adapter1, adapter2, classes)
        }
      }
    }
  }

  fun doesNotDependOn(packageName: String, classes: JavaClasses) {
    denyDependency(basePackage, packageName, classes)
  }

  fun doesNotContainEmptyPackages() {
    denyEmptyPackages(allAdapterPackages())
  }
}
