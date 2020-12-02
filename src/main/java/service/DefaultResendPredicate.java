package service;

import java.net.http.HttpResponse;
import java.util.function.Predicate;

public class DefaultResendPredicate implements Predicate<HttpResponse> {

  @Override
  public boolean test(final HttpResponse response) {
    return response != null && response.statusCode() == 503;
  }
}
