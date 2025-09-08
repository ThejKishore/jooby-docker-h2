package app;

import io.jooby.Jooby;
import io.jooby.ServerOptions;
import io.jooby.SslOptions;
import io.jooby.jetty.JettyServer;
import io.jooby.output.OutputOptions;
import org.eclipse.jetty.util.thread.QueuedThreadPool;

import java.util.concurrent.Executors;


public class App  {

    public static void main(final String[] args) {
        System.setProperty("logback.configurationFile", "./conf/logback.xml");
        var threadPool = new QueuedThreadPool();
        threadPool.setReservedThreads(0);
        threadPool.setVirtualThreadsExecutor(Executors.newVirtualThreadPerTaskExecutor());
        var options = new ServerOptions()
                .setOutput(OutputOptions.defaults())
                .setCompressionLevel(6)
                .setPort(8080)
                .setIoThreads(16)
                .setWorkerThreads(64)
                .setCompressionLevel(ServerOptions.DEFAULT_COMPRESSION_LEVEL)
                .setDefaultHeaders(true)
                .setMaxRequestSize(10485760)
                .setSecurePort(8433)
                .setSsl(SslOptions.selfSigned())
                .setHttpsOnly(false)
                .setHttp2(true)
                .setExpectContinue(true);
        Jooby.runApp(args, new JettyServer(options,threadPool), Routing::new);
    }
}
