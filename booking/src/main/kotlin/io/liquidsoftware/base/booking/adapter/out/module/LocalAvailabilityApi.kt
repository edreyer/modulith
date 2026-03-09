package io.liquidsoftware.base.booking.adapter.out.module

import io.liquidsoftware.base.booking.application.port.`in`.AvailabilityApi
import io.liquidsoftware.base.booking.application.port.`in`.GetAvailabilityQuery
import io.liquidsoftware.common.context.ModuleApiRegistry

class LocalAvailabilityApi : AvailabilityApi {
  override suspend fun getAvailability(query: GetAvailabilityQuery) =
    ModuleApiRegistry.require(AvailabilityApi::class).getAvailability(query)
}
