package org.pmiops.databrowser

import io.gatling.core.Predef.scenario
import io.gatling.core.structure.ScenarioBuilder

import scala.concurrent.duration.{FiniteDuration, _}
import scala.language.postfixOps

case class ConfigurableScenario(name: String,
                                builder: ScenarioBuilder,
                                users: Int,
                                time: FiniteDuration)

sealed trait UserScenario { val name: String }
case object HomePage extends UserScenario { val name = "Home Page" }
case object UserStorySmoking extends UserScenario { val name = "User Story: Smoking" }

/**
  * User scenarios are defined here. Scenarios can be comprised of different Pages.
  *
  * Scenario names must be unique which is why there is a bit of name duplication.
  * In the case where we want to run the same scenario under multiple conditions,
  * (i.e. different #s of users across different ramp up times) we have to give
  * each condition a unique name, but also need to calculate that name based on the
  * configuration of the run.
  */
object Scenarios {

  val homePage: ScenarioBuilder = scenario(HomePage.name)
    .exec(Pages.Home.home)

  val searchSmoke: ScenarioBuilder = scenario(UserStorySmoking.name)
    .exec(Pages.Home.home)
    .exec(Pages.Search.search("smoke"))
    .exec(Pages.ViewProcedures.view("smoke"))
    .exec(Pages.Home.home)

  private val terms: List[String] = List("smoke", "diabetes", "cancer", "heart disease")
  val domainSearchApiScenarios: List[ConfigurableScenario] = terms.flatMap { t =>
    Configuration.userRampUpTimes.map {
      case(users: Int, time: FiniteDuration) =>
        val name: String = s"Domain Search: $t: users: $users; time: $time"
        val builder: ScenarioBuilder = scenario(name).exec(Pages.APIs.domainSearch(t))
        ConfigurableScenario(name, builder, users, time)
    }
  }

  val configuredScenarios: List[ConfigurableScenario] = List(
    ConfigurableScenario(HomePage.name, homePage, 10, 10 seconds),
    ConfigurableScenario(UserStorySmoking.name, searchSmoke, 10, 10 seconds)
  ) ::: domainSearchApiScenarios

}
