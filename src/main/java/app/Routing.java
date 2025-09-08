package app;

import com.codahale.metrics.health.jvm.ThreadDeadlockHealthCheck;
import com.codahale.metrics.jvm.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jooby.Context;
import io.jooby.GracefulShutdown;
import io.jooby.Jooby;
import io.jooby.Route;
import io.jooby.handler.HeadHandler;
import io.jooby.jackson.JacksonModule;
import io.jooby.metrics.MetricsModule;
import org.jdbi.v3.core.Jdbi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.List;


public class Routing extends Jooby {

    private static final Logger log = LoggerFactory.getLogger(Routing.class);
    private static Jdbi jdbi = Jdbi.create("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=false"); // (H2 in-memory database kept alive)

    public Routing() {
        install(new JacksonModule(new ObjectMapper()));
        install(new MetricsModule()
                .threadDump()
                .ping()
                .healthCheck("deadlock", new ThreadDeadlockHealthCheck())
                .metric("memory", new MemoryUsageGaugeSet())
                .metric("threads", new ThreadStatesGaugeSet())
                .metric("gc", new GarbageCollectorMetricSet())
                .metric("fs", new FileDescriptorRatioGauge()));
        install(new GracefulShutdown(Duration.ofMinutes(1)));
        use(new HeadHandler());


        use(next -> ctx -> logTimeTakenFilter(next, ctx));

        //initialize the database
        jdbi.withHandle(handle -> {
            handle.execute("CREATE TABLE IF NOT EXISTS users (id INTEGER PRIMARY KEY, name VARCHAR)");

            // Inline positional parameters
            handle.execute("MERGE INTO users (id, name) KEY(id) VALUES (?, ?)", 0, "Alice");

            // Positional parameters
            handle.createUpdate("MERGE INTO users (id, name) KEY(id) VALUES (?, ?)")
                    .bind(0, 1) // 0-based parameter indexes
                    .bind(1, "Bob")
                    .execute();

            // Named parameters
            handle.createUpdate("MERGE INTO users (id, name) KEY(id) VALUES (:id, :name)")
                    .bind("id", 2)
                    .bind("name", "Clarice")
                    .execute();

            // Named parameters from explicit binds (records aren't treated as beans by default)
            handle.createUpdate("MERGE INTO users (id, name) KEY(id) VALUES (:id, :name)")
                    .bind("id", 3)
                    .bind("name", "David")
                    .execute();

            // Easy mapping to any type
            return null;
        });


        get("/", this::getHandler);
        get("/users", this::getUsersHandler);
        get("/users/{id}", this::getUserByIdHandler);
        post("/users", this::createUser);
        put("/users/{id}", this::updateUser);

        onStarted(() -> {
            log.info("--------------------------------------------------");
            log.info("Application '{}' is ready!", getName());
            log.info("Listening http on: http://localhost:{}", getServerOptions().getPort());
            log.info("Listening https on: http://localhost:{}", getServerOptions().getSecurePort());
            log.info("--------------------------------------------------");
        });

//        onStop(() -> {
//            log.info("--------------------------------------------------");
//            log.info("Application '{}' is stopping...", getName());
//        });
    }



    private static Object logTimeTakenFilter(Route.Handler next, Context ctx) throws Exception {
        long start = System.nanoTime();
        Object response = next.apply(ctx);
        long end = System.nanoTime();
        long took =end - start;
        log.info(" {} \"{} {}\" time took: {} nanoseconds", ctx.getRemoteAddress() , ctx.getMethod(), ctx.getRequestPath(), took);
        return response;
    }

    private String getHandler(Context ctx) {
        return "Welcome to Jooby!";
    }

    private List<User> getUsersHandler(Context ctx) {
        return jdbi.withHandle(handle -> handle.createQuery("SELECT * FROM users ORDER BY name")
                    .registerRowMapper(User.class ,
                            (rs,ctx1) -> new User(rs.getInt("id"),rs.getString("name")))
                    .mapTo(User.class)
                    .list());
    }

    private User createUser(Context ctx){
        return jdbi.withHandle(handle -> {
            User user = ctx.body(User.class);
            handle.createUpdate("INSERT INTO users (id, name) VALUES (:id, :name)")
                    .bind("id", user.id())
                    .bind("name", user.name())
                    .execute();
            return user;
        });
    }

    private User getUserByIdHandler(Context ctx) {
        int id = ctx.path("id").intValue();
        return jdbi.withHandle(handle -> handle.createQuery("SELECT * FROM users where id = :id")
                .bind("id", id)
                .registerRowMapper(User.class ,
                        (rs,ctx1) -> new User(rs.getInt("id"),rs.getString("name")))
                .mapTo(User.class)
                .one());
    }

    private User updateUser(Context ctx){
        return jdbi.withHandle(handle -> {
            int id = ctx.path("id").intValue();
            User user = ctx.body(User.class);
            handle.createUpdate("Update users SET name = :name WHERE id = :id")
                    .bind("id", id)
                    .bind("name", user.name())
                    .execute();
            return user;
        });
    }
}
