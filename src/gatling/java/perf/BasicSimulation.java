package perf;

import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.*;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gatling.javaapi.core.*;
import io.gatling.javaapi.http.*;

public class BasicSimulation extends Simulation {


    ObjectMapper objectMapper = new ObjectMapper();

    HttpProtocolBuilder httpProtocol = http
            .baseUrl(System.getProperty("targetBaseUrl", "http://localhost:8080"))
            .acceptHeader("application/json")
            .userAgentHeader("Gatling/Java");

    ScenarioBuilder scn = scenario("Basic Jooby App Scenario")
            .exec(
                    http("GET /")
                            .get("/")
                            .check(status().is(200))
            )
            .pause(1)
            .exec(
                    http("GET /users")
                            .get("/users")
                            .check(status().in(200, 404))
            )
            .pause(1)
            .exec(s -> {
                Session ns = null;
                try {
                    ns = s.set("userData", objectMapper.writeValueAsString(new User(null, "Test")));
                    System.out.println(ns.getString("userData"));
                } catch (JsonProcessingException e) {
                    throw new RuntimeException(e);
                }
                return ns;
            })
            .exec(
                    http("POST /users")
                            .post("/users")
                            .body(StringBody("${userData}")).asJson()
                            .check(status().in(200,201))


            );

    public BasicSimulation() throws JsonProcessingException {
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        setUp(
                scn.injectOpen(
                        rampUsers(10).during(10),
                        constantUsersPerSec(5).during(10)

                )
        ).protocols(httpProtocol);
    }

    record User(Long id, String name) {
    }
}
