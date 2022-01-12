package io.liquidsoftware.base.booking.application.config

import io.liquidsoftware.base.booking.application.port.`in`.CancelAppointmentCommand
import io.liquidsoftware.base.booking.application.port.`in`.ScheduleAppointmentCommand
import io.liquidsoftware.base.user.bridgekeeper.UserResourceTypes
import io.liquidsoftware.base.user.bridgekeeper.UserRoles
import io.liquidsoftware.bridgekeeper.BridgeKeeper
import io.liquidsoftware.bridgekeeper.Visibility
import io.liquidsoftware.bridgekeeper.fns.className
import io.liquidsoftware.bridgekeeper.rolesPermissionRegistry
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
internal class BookingBridgekeeperConfig {

  companion object {
    const val BOOKING_BRIDGE_KEEPER = "bookingBridgeKeeper"
  }

  // Bridgekeeper
  @Bean(io.liquidsoftware.base.booking.application.config.BookingBridgekeeperConfig.Companion.BOOKING_BRIDGE_KEEPER)
  fun bookingBridgeKepper() = rolesPermissionRegistry {
    role(UserRoles.ROLE_ACTIVE_USER) {
      resourceType(UserResourceTypes.MY_USER) {
        visibility = Visibility.VISIBLE
        operations {
          +className<ScheduleAppointmentCommand>()
          +className<CancelAppointmentCommand>()
        }
      }
    }
  }.let { BridgeKeeper(it) }

}
