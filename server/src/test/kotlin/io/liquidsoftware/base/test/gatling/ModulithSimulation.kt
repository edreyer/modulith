package io.liquidsoftware.base.test.gatling

import io.gatling.javaapi.core.CoreDsl.StringBody
import io.gatling.javaapi.core.CoreDsl.atOnceUsers
import io.gatling.javaapi.core.CoreDsl.constantUsersPerSec
import io.gatling.javaapi.core.CoreDsl.csv
import io.gatling.javaapi.core.CoreDsl.doWhile
import io.gatling.javaapi.core.CoreDsl.exec
import io.gatling.javaapi.core.CoreDsl.global
import io.gatling.javaapi.core.CoreDsl.jsonPath
import io.gatling.javaapi.core.CoreDsl.scenario
import io.gatling.javaapi.core.Simulation
import io.gatling.javaapi.http.HttpDsl.http
import io.gatling.javaapi.http.HttpDsl.status
import java.time.LocalDate
import java.time.format.DateTimeFormatter


class ModulithSimulation: Simulation() {
  val usersSingle = csv("gatling-users.csv")
  val usersCircular = csv("gatling-users.csv").circular()

  var formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
  val dates = generateSequence(mapOf("date" to LocalDate.now())) {
    mapOf("date" to it["date"]!!.plusDays(1))
  }
    .map { mapOf("date" to it["date"]!!.format(formatter)) }
    .iterator()


  val userCount = usersSingle.recordsCount()

  val httpProtocol = http
    .baseUrl("http://localhost:8080")
    .acceptHeader("application/json")
    .contentTypeHeader("application/json")

  val registerUsers = exec(
    http("register")
      .post("/user/register")
      .body(StringBody("""{
          "msisdn" : "#{msisdn}",
          "email" : "#{email}",
          "password" : "password",
          "role" : "ROLE_USER"
        }""".trimIndent())).asJson()
      .check(
        status().`in`(200, 400),
      )
      .checkIf{ response, _ -> 400 == response.status().code() }.then(
        jsonPath("$.errors").ofString().isEL("User #{msisdn} exists"),
      )
  )

  private val registerScenario = scenario("Register Users")
    .feed(usersSingle)
    .exec(registerUsers)

  private val bookAppointment = exec(
    // login
    http("book appointment")
      .post("/auth/login")
      .body(StringBody("""{
          "username" : "#{email}",
          "password" : "#{password}"
        }""".trimIndent())).asJson()
      .check(
        status().shouldBe(200),
        jsonPath("$.accessToken").saveAs("token")
      ),
    // get availability
    doWhile{ session ->
      session.getString("available_time") == null
    }.on(
      http("get appt times")
        .get("/api/v1/availability/#{date}")
        .header("Authorization", "Bearer #{token}")
        .check(
          status().shouldBe(200),
          jsonPath("$.times[0]").exists().saveAs("available_time")
        )
    ),
    // book appt
    http("schedule appt")
      .post("/api/v1/appointments/schedule")
      .body(StringBody("""{
          "duration" : "30",
          "scheduledTime" : "#{date}T#{available_time}",
          "workOrder" : {
            "service" : "service"
          }
        }""".trimIndent())).asJson()
      .header("Authorization", "Bearer #{token}")
      .check(
        status().shouldBe(200),
        jsonPath("$.appointment.id")
          .transform { id -> id.subSequence(0, 3) }
          .shouldBe("a__")
      )

  )

  private val bookAppointmentScenario = scenario("Book Appointment")
    .feed(usersCircular)
    .feed(dates)
    .exec(bookAppointment)


  init {
    this.setUp(
      registerScenario.injectOpen(atOnceUsers(userCount))
        .andThen(bookAppointmentScenario.injectOpen(
          constantUsersPerSec(100.0).during(30)
        ))
    )
      .protocols(httpProtocol)
      .assertions(global().failedRequests().count().shouldBe(0L))
  }

}
