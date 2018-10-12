package computerdatabase

import io.gatling.core.Predef._
import io.gatling.core.structure.{ChainBuilder, ScenarioBuilder}
import io.gatling.http.Predef._
import io.gatling.http.protocol.HttpProtocolBuilder


class BasicSimulation extends Simulation {

  val httpProtocol: HttpProtocolBuilder = http
    .baseUrl("http://computer-database.gatling.io")
    .inferHtmlResources(BlackList(""".*.css""", """.*.js""", """.*.ico"""), WhiteList())
    .acceptHeader("*/*")
    .userAgentHeader("curl/7.54.0")

  val defaultHeaders = Map("Proxy-Connection" -> "Keep-Alive")

  object Search {

    val search: ChainBuilder = exec(http("Home")
      .get("/")
      .headers(defaultHeaders))
      .pause(1)
      .exec(http("Search")
        .get("/computers?f=macbook")
        .headers(defaultHeaders))
      .pause(1)
      .exec(http("Select")
        .get("/computers/6")
        .headers(defaultHeaders))
      .pause(1)
  }

  object Browse {

    val browse: ChainBuilder = exec(http("Home")
      .get("/computers")
      .headers(defaultHeaders))
      .pause(1)
      .exec(http("Page 1")
        .get("/computers?p=1")
        .headers(defaultHeaders))
      .pause(1)
      .exec(http("Page 2")
        .get("/computers?p=2")
        .headers(defaultHeaders))
      .pause(1)
      .exec(http("Page 3")
        .get("/computers?p=3")
        .headers(defaultHeaders))
      .pause(1)
  }

  object Edit {

    val edit: ChainBuilder = exec(http("Home")
      .get("/computers")
      .headers(defaultHeaders))
      .pause(1)
      .exec(http("New Computer")
        .get("/computers/new")
        .headers(defaultHeaders))
      .pause(1)
      .exec(http("Submit Form")
        .post("/computers")
        .headers(defaultHeaders)
        .formParam("name", "Archie")
        .formParam("introduced", "2018-10-12")
        .formParam("company", "36"))
      .pause(1)
  }

  val scn: ScenarioBuilder = scenario("BasicSimulation")
      .exec(Search.search, Browse.browse, Edit.edit)

  setUp(scn.inject(atOnceUsers(1))).protocols(httpProtocol)

}