package org.pmiops.databrowser

import scala.concurrent.duration._
import scala.language.postfixOps

object Configuration {

  val env: String = System.getProperty("env", "stable")
  val defaultPause: Int = 3
  val defaultMaxResponseTime: Int = 15000
  val defaultFailedRequestsLimit: Int = 0
  val userAgentHeader: String = "Gatling Performance Testing"
  val defaultUrl: String = {
    env match {
      case "prod" => "https://public.api.researchallofus.org"
      case "stable" => "https://public.api.stable.fake-research-aou.org"
      case "staging" => "https://api-dot-aou-db-staging.appspot.com"
      case "test" => "https://api-dot-aou-db-test.appspot.com"
      case "local" => "http://localhost:8083"
      case _ => "https://public.api.stable.fake-research-aou.org"
    }
  }
  val userRampUpTimes: Map[Int, FiniteDuration] = Map(
    10 -> (10 seconds),
    100 -> (60 seconds),
    1000 -> (120 seconds)
  )

}
