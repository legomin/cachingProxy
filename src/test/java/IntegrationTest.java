import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import com.sun.net.httpserver.HttpServer;

public class IntegrationTest {

  public static void main(String[] args) {
    final AtomicInteger requestCount = new AtomicInteger();
    final HttpServer mockBackend;

    try {
      /*
      backend emulator
      sometimes returns 200,
      sometimes returns 503,
      sometimes just sleeps
       */
      mockBackend = HttpServer.create(new InetSocketAddress(5555), 0);
      mockBackend.createContext("/", request -> {
        final int requestNumber = requestCount.incrementAndGet();
        try {
          if (requestNumber % 1000 == 0) {
            Thread.sleep(3000);
          }
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
        String response = "OK";
        int code;
        if (requestNumber <= 2000 ) {
          code = 503;
        } else {
          code = 200;
        }
        request.sendResponseHeaders(code, response.getBytes(StandardCharsets.UTF_8).length);
        OutputStream os = request.getResponseBody();
        os.write(response.getBytes());
        os.close();
      });
      mockBackend.setExecutor(null);

      mockBackend.start();

      //starting proxy
      App app = new App("http://localhost", 5555, 7777, 10, 10);
      app.start();

      //starting client bots
      HttpClient client = HttpClient.newHttpClient();
      ExecutorService executorService = Executors.newFixedThreadPool(10);
      URI uri = new URI("http://localhost:7777");
      //and make them send a bunch of requests
      for (int i = 0; i < 10000; i++) {
        int finalI = i;
        executorService.execute(() -> {
          try {
            client.send(HttpRequest.newBuilder()
              .uri(uri)
              .POST(HttpRequest.BodyPublishers.ofByteArray(("message " + finalI)
              .getBytes(StandardCharsets.UTF_8)))
              .build(), r -> HttpResponse.BodySubscribers.discarding());
          } catch (IOException | InterruptedException e) {
            e.printStackTrace();
          }
        });
      }

      Thread.sleep(40000);
      System.out.println("got requests: " + requestCount.get());
      assert requestCount.get() == 12000; // wow, works. MAGIC))

    } catch (IOException | URISyntaxException | InterruptedException e) {
      e.printStackTrace();
    }

  }

}
