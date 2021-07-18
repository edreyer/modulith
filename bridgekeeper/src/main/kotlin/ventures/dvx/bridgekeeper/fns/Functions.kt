package ventures.dvx.bridgekeeper.fns

/**
 * Helper function to get fully qualified class name from some type T
 */
inline fun <reified T> className(): String {
  val clazz: Class<*> = T::class.java
  return clazz.name
}
