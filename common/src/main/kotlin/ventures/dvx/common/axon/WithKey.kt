package ventures.dvx.common.axon

interface WithKey {

  /**
   * Most (all?) aggregates have business data that makes that aggregate unique.
   * This is distinct from a generated ID, such as a UUID.
   * As an example, US citizens have a social security number.
   * Depending on your business context, a phone number could make an aggregate unique,
   * or an address.
   * It's also possible a tuple of values makes an aggregate unique.
   * For example. firstName + lastName + email (contrived example)
   *
   * This method should return a string that makes this aggregate instance unique within the
   * aggregate class.
   */
  fun businessKey(): String
}
