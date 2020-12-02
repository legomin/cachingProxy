package service;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.function.Function;

import domain.Data;

/**
 * Sends data through http to backend
 *
 * return response from backend
 * if operation finished exceptionally - throw exception, wrapped in runtime exception (because of how Function constructed)
 */
public class HttpRequestRedirectAction implements Function<Data, HttpResponse> {
  private final HttpClient httpClient;
  private final URI uri;

  public HttpRequestRedirectAction(final String outgoingHost, final Integer outgoingPort) throws URISyntaxException {
    this.uri = new URI(outgoingHost + ':' + outgoingPort);
    this.httpClient = HttpClient.newHttpClient();
  }

  @Override
  public HttpResponse apply(final Data data) {
    final HttpRequest request = HttpRequest.newBuilder().uri(uri).method(data.getMethod(),
      HttpRequest.BodyPublishers.ofByteArray(data.getData().getBytes(StandardCharsets.UTF_8))).build();
    try {
      return httpClient.send(request, responseInfo -> HttpResponse.BodySubscribers.discarding());
    } catch (final Exception e) {
      throw new RuntimeException(e);
    }
  }
}
