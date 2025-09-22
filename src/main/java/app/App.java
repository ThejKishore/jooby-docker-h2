package app;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import io.jooby.Jooby;
import io.jooby.ServerOptions;
import io.jooby.SslOptions;
import io.jooby.jetty.JettyServer;
import io.jooby.output.OutputOptions;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.slf4j.Logger;

import java.util.concurrent.Executors;


public class App  {


    private static final Logger log = org.slf4j.LoggerFactory.getLogger(App.class);

    public static void main(final String[] args) {
        Config config = ConfigFactory.load();
        System.setProperty("logback.configurationFile", "./conf/logback.xml");
        log.info("Starting app with config: {}", config.getInt("app.port"));
        Jooby.runApp(args, new JettyServer(serverOptions(config),threadPool(config)), Routing::new);
    }


    private static ServerOptions serverOptions(Config config){
        return new ServerOptions()
                .setOutput(OutputOptions.defaults())
                .setCompressionLevel(6)
                .setPort(config.getInt("app.port"))
                .setIoThreads(config.getInt("app.server.ioThreads"))
                .setWorkerThreads(64)
                .setCompressionLevel(ServerOptions.DEFAULT_COMPRESSION_LEVEL)
                .setDefaultHeaders(true)
                .setMaxRequestSize(10485760)
                .setSecurePort(8433)
                .setSsl(SslOptions.selfSigned())
                .setHttpsOnly(false)
                .setHttp2(true)
                .setExpectContinue(true);
    }

    private static QueuedThreadPool threadPool(Config config) {
        var threadPool =  new QueuedThreadPool();
        threadPool.setReservedThreads(0);
        threadPool.setVirtualThreadsExecutor(Executors.newVirtualThreadPerTaskExecutor());
        return threadPool;
    }
}
