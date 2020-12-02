package web;

import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;
import java.util.function.Function;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import domain.Data;

public class RestServer {

  private final HttpServer server;

  public RestServer(final Integer incomingPort, final ExecutorService executorService, final Consumer<Data> service,
    final Function<HttpExchange, Data> dataExtractor) throws IOException {

    final Consumer<Data> notNullService = requireNonNull(service);
    final Function<HttpExchange, Data> notNullDataExtractor = requireNonNull(dataExtractor);
    final ExecutorService notNullExecutor = requireNonNull(executorService);
    final String response = "OK";
    this.server = HttpServer.create(new InetSocketAddress(requireNonNull(incomingPort)), 0);
    server.createContext("/", request -> {
      final Data data = notNullDataExtractor.apply(request);
      notNullService.accept(data);
      request.sendResponseHeaders(200, response.getBytes(StandardCharsets.UTF_8).length);
      final OutputStream os = request.getResponseBody();
      os.write(response.getBytes());
      os.close();
    });
    server.setExecutor(notNullExecutor);
  }

  public void start() {
    server.start();
  }

  public void stop(int timeout) {
    server.stop(timeout);
  }


}
