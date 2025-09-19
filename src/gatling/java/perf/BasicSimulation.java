package perf;

import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.*;

import app.User;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.gatling.javaapi.core.*;
import io.gatling.javaapi.http.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BasicSimulation extends Simulation {


    private static final Logger log = LoggerFactory.getLogger(BasicSimulation.class);
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
                log.info("==========> Setting up user data");
                Session ns = null;
                try {
                    var data = objectMapper.writeValueAsString(new User(-1, "Test"));
                    log.info("-------json---- {}",data);
                    ns = s.set("userData", data);
                    log.info("session data {}",ns.getString("userData"));
                    return ns;
                } catch (JsonProcessingException e) {
                    throw new RuntimeException(e);
                }
            })
            .exec(
                    http("POST /users")
                            .post("/users")
                            .body(StringBody("${userData}")).asJson()
                            .check(bodyString().saveAs("user"))
                            .check(status().in(200,201))
                            .check(jsonPath("$.id").notNull())
            )
            .exec(session -> {
                try {
                    log.info("response recived {}",session.getString("user"));
                    User user = objectMapper.readValue((String) session.get("user"), User.class);
                    User updatedUser = new User(user.id(), "Updated"+" "+user.name());
                    session = session.set("user_id", user.id());
                    session = session.set("userData", objectMapper.writeValueAsString(updatedUser));
                }catch (Exception e){
                    throw new RuntimeException(e);
                }
                return session;
            })
            .pause(1)
            .exec(
                    http("PUT /users/id")
                            .put("/users/${user_id}")
                            .body(StringBody("${userData}")).asJson()
                            .check(status().in(200,201))
                            .check(jsonPath("$.name").notNull())
            );

    public BasicSimulation() throws JsonProcessingException {
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        setUp(
                scn.injectOpen(
//                        atOnceUsers(1)
                        rampUsers(10).during(10),
                        constantUsersPerSec(10).during(10)

                )
        ).protocols(httpProtocol);
    }

}
