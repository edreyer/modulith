package ventures.dvx.base.booking.application.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import ventures.dvx.base.booking.application.port.`in`.CancelAppointmentCommand
import ventures.dvx.base.booking.application.port.`in`.ScheduleAppointmentCommand
import ventures.dvx.base.user.bridgekeeper.UserResourceTypes
import ventures.dvx.base.user.bridgekeeper.UserRoles
import ventures.dvx.bridgekeeper.BridgeKeeper
import ventures.dvx.bridgekeeper.Visibility
import ventures.dvx.bridgekeeper.fns.className
import ventures.dvx.bridgekeeper.rolesPermissionRegistry

@Configuration
internal class BookingBridgekeeperConfig {

  companion object {
    const val BOOKING_BRIDGE_KEEPER = "bookingBridgeKeeper"
  }

  // Bridgekeeper
  @Bean(BOOKING_BRIDGE_KEEPER)
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
