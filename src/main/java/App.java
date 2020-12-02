import java.io.IOException;
import java.net.URISyntaxException;
import java.net.http.HttpResponse;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

import com.sun.net.httpserver.HttpExchange;

import domain.Data;
import service.DefaultResendPredicate;
import service.HttpRequestRedirectAction;
import service.MultithreadedRetryingService;
import web.DefaultDataExtractor;
import web.RestServer;

/**
 * Works as:
 *
 * - Starts Http server in one thread pool,
 * - Sending http data to backend in another thread pool
 */
public class App {
  private static final int DEFAULT_INCOMING_PORT = 7777;
  private static final int DEFAULT_OUTGOING_PORT = 8000;
  private static final int DEFAULT_INCOMING_THREADS_COUNT = 4;
  private static final int DEFAULT_OUTGOING_THREADS_COUNT = 4;

  private final RestServer server;
  private final ExecutorService incomingExecutorService;
  private final ExecutorService outgoingExecutorService;

  public App(final String outgoingHost, final Integer outgoingPort, final Integer incomingPort,
    final Integer incomingThreadsCount, final Integer outgoingThreadsCount) throws URISyntaxException, IOException {

    //initializing service
    this.outgoingExecutorService = Executors.newFixedThreadPool(outgoingThreadsCount);
    final Function<Data, HttpResponse> action = new HttpRequestRedirectAction(outgoingHost, outgoingPort);
    final Predicate<HttpResponse> needToRetry = new DefaultResendPredicate();
    final Consumer<Data> service = new MultithreadedRetryingService<>(action, needToRetry, outgoingExecutorService,
      null);

    //initializing rest api
    this.incomingExecutorService = Executors.newFixedThreadPool(incomingThreadsCount);
    final Function<HttpExchange, Data> dataExtractor = new DefaultDataExtractor();
    this.server = new RestServer(incomingPort, incomingExecutorService, service, dataExtractor);
  }

  public void start() {
    server.start();
  }

  public void stop(int timeout) {
    server.stop(timeout);
    incomingExecutorService.shutdown();
    outgoingExecutorService.shutdown();
  }

  public static void main(String[] args) throws Exception {
    assert args.length >= 1; // expecting outgoing host is not empty

    final String outgoingHost = args[0];

    final int outgoingPort = getOptionalArg(args, 1).orElse(DEFAULT_OUTGOING_PORT);
    final int incomingPort = getOptionalArg(args, 2).orElse(DEFAULT_INCOMING_PORT);
    final int incomingThreadsCount = getOptionalArg(args, 3).orElse(DEFAULT_INCOMING_THREADS_COUNT);
    final int outgoingThreadsCount = getOptionalArg(args, 4).orElse(DEFAULT_OUTGOING_THREADS_COUNT);

    App app = new App(outgoingHost, outgoingPort, incomingPort, incomingThreadsCount, outgoingThreadsCount);

    app.start();
  }

  private static Optional<Integer> getOptionalArg(final String[] args, final int index) {
    if (args.length > index) {
      return Optional.of(Integer.parseInt(args[index]));
    }
    return Optional.empty();
  }

}
