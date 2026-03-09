package io.liquidsoftware.common.context

import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass
import org.springframework.beans.factory.DisposableBean

object ModuleApiRegistry {
  private val apis = ConcurrentHashMap<KClass<*>, Any>()

  fun <T : Any> register(type: KClass<T>, api: T): ModuleApiRegistration<T> {
    apis[type] = api
    return ModuleApiRegistration(type, api)
  }

  @Suppress("UNCHECKED_CAST")
  fun <T : Any> require(type: KClass<T>): T =
    apis[type] as? T
      ?: throw IllegalStateException("No module API registered for ${type.qualifiedName}")

  internal fun <T : Any> unregister(type: KClass<T>, api: T) {
    apis.remove(type, api)
  }
}

class ModuleApiRegistration<T : Any> internal constructor(
  private val type: KClass<T>,
  private val api: T,
) : DisposableBean {
  override fun destroy() {
    ModuleApiRegistry.unregister(type, api)
  }
}
