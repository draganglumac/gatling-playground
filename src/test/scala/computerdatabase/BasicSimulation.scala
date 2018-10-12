package computerdatabase

import io.gatling.core.Predef._
import io.gatling.core.feeder.SourceFeederBuilder
import io.gatling.core.structure.{ChainBuilder, ScenarioBuilder}
import io.gatling.http.Predef._
import io.gatling.http.protocol.HttpProtocolBuilder

import scala.concurrent.duration._

class BasicSimulation extends Simulation {

  val httpProtocol: HttpProtocolBuilder = http
    .baseUrl("http://computer-database.gatling.io")
    .inferHtmlResources(BlackList(""".*.css""", """.*.js""", """.*.ico"""), WhiteList())
    .acceptHeader("*/*")
    .userAgentHeader("curl/7.54.0")

  val defaultHeaders = Map("Proxy-Connection" -> "Keep-Alive")

  // splitting the monolith into separate processes

  object Search {

    // introduce random data to make scenario more realistic
    val feeder: SourceFeederBuilder[String] = csv("search.csv").random

    val search: ChainBuilder = exec(http("Home")
      .get("/")
      .headers(defaultHeaders))
      .pause(1)
      .feed(feeder)
      .exec(http("Search")
        .get("/computers?f=${searchCriterion}")
        .headers(defaultHeaders)
        .check(css("a:contains('${searchComputerName}')", "href").saveAs("computerURL")))
      .pause(1)
      .exec(http("Select")
        .get("${computerURL}")
        .headers(defaultHeaders))
      .pause(1)
  }

  object Browse {

    // DRY - avoid code duplication

    def goToPage(page: Int): ChainBuilder = exec(http("Page " + page)
      .get("/computers?p=" + page)
      .headers(defaultHeaders))
      .pause(1)

    val browse: ChainBuilder = exec(
      goToPage(0),
      goToPage(1),
      goToPage(2),
      goToPage(3),
      goToPage(4))
  }

  object Edit {

    import java.util.concurrent.ThreadLocalRandom

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
        .formParam("company", "36")
        // introduce some random failures - as the server always returns 200, it will be treated as a failure when expected code is 201
        .check(status.is(session => 200 + ThreadLocalRandom.current.nextInt(2))))
      .pause(1)


    val tryMaxEdit: ChainBuilder = tryMax(2) {
      exec(edit)
    }.exitHereIfFailed

  }

  // Adding users
  val users: ScenarioBuilder = scenario("Users").exec(Search.search, Browse.browse)
  val admins: ScenarioBuilder = scenario("Admins").exec(Search.search, Browse.browse, Edit.tryMaxEdit)

  setUp(
    users.inject(rampUsers(10) during (10 seconds)),
    admins.inject(rampUsers(2) during (10 seconds))
  ).protocols(httpProtocol)

}