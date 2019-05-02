package org.pmiops.databrowser

import java.net.URLEncoder
import java.nio.charset.StandardCharsets

import io.gatling.core.Predef._
import io.gatling.core.json.Json
import io.gatling.core.structure.ChainBuilder
import io.gatling.http.Predef._
import io.gatling.http.request.builder.HttpRequestBuilder

/**
  * A "Page" should be a collection of API calls that are made when navigating to a specific
  * destination or performing a limited set of actions. These should be composable so larger
  * scenarios can be built around them - attempt to keep them limited in scope.
  */
object Pages {

  object APIs {
    val config: HttpRequestBuilder = http("config").get("/v1/config")
    val genderCount: HttpRequestBuilder = http("gender-count")
      .get("/v1/databrowser/gender-count")
    def conceptAnalysisResults (conceptIds: Seq[String]): HttpRequestBuilder = {
      http("concept-analysis-results")
        .get("/v1/databrowser/concept-analysis-results?concept-ids=" + conceptIds.mkString(","))
    }
    val participantCount: HttpRequestBuilder = http("participant-count").get("/v1/databrowser/participant-count")
    val domainTotals: HttpRequestBuilder = http("domain-totals").get("/v1/databrowser/domain-totals")
    def domainSearch (searchTerm: String): HttpRequestBuilder = {
      def encodedTerm: String = URLEncoder.encode(searchTerm, StandardCharsets.UTF_8.toString).toLowerCase
      http("domain-search").get("/v1/databrowser/domain-search?searchWord=" + encodedTerm)
    }
    def searchConcepts(postBody: String): HttpRequestBuilder = {
      http("search-concepts")
        .post("/v1/databrowser/searchConcepts")
        .header("Content-Type", "application/json")
        .body(StringBody(postBody))
    }
  }

  object Home {
    val home: ChainBuilder = exec(APIs.config)
      .exec(APIs.genderCount.check(status.is(session => 200)))
      .exec(APIs
        .conceptAnalysisResults(Seq("903118","903115","903133","903121","903135","903136","903126","903111","903120"))
        .check(status.is(session => 200)))
      .exec(APIs.participantCount.check(status.is(session => 200)))
      .exec(APIs.domainTotals.check(status.is(session => 200)))
      .pause(Configuration.defaultPause)
  }

  object Search {
    def search(searchTerm: String): ChainBuilder = exec(APIs.domainSearch(searchTerm).check(status.is(session => 200)))
      .pause(Configuration.defaultPause)
  }

  object ViewProcedures {
    def view (searchTerm: String): ChainBuilder = {
      val postMap = Map(
        "query" -> searchTerm,
        "domain" -> "PROCEDURE",
        "standardConceptFilter" -> "STANDARD_OR_CODE_ID_MATCH",
        "maxResults" -> 100,
        "minCount" -> 1
      )
      exec(APIs.participantCount)
        .exec(APIs.searchConcepts(Json.stringify(postMap)))
        .pause(Configuration.defaultPause)
    }
  }

}
