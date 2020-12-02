package web;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.function.Function;
import java.util.logging.Logger;

import com.sun.net.httpserver.HttpExchange;

import domain.Data;

/**
 * Just extracts method & body from http request
 */
public class DefaultDataExtractor implements Function<HttpExchange, Data> {
  private static final Logger logger = Logger.getLogger(DefaultDataExtractor.class.getName());

  @Override
  public Data apply(final HttpExchange request) {
    final String data;
    try {
      data = new String(request.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
      final String method = request.getRequestMethod();
      return new Data(method, data);
    } catch (final IOException e) {
      logger.warning(String.format("Failed to extract data from request %s, \n%s", request, e));
      return null;
    }
  }
}
