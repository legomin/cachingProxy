package service;

import static java.util.Objects.requireNonNull;

import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * The main tasks queue class
 *
 * 1. get T data
 * 2. process it in executor by T -> U function
 * 3. analyze U result if it need to process again (by Predicate<U>), if so - puts data to execution queue again
 * 4. analyze Throwable if process is finished exceptionally (by Predicate<Throwable>),  if so - puts data to execution queue again
 *
 * @param <T> - type of incoming data
 * @param <U> - type of result of data processing
 */

public class MultithreadedRetryingService<T, U> implements Consumer<T> {
  private final Function<T, U> action;
  private final ExecutorService executorService;
  private final Predicate<U> needToRetry;
  private final Predicate<Throwable> exceptionalNeedToRetry;

  public MultithreadedRetryingService(final Function<T, U> action, final Predicate<U> needToRetry,
    final ExecutorService executorService, final Predicate<Throwable> exceptionalNeedToRetry) {
    this.action = requireNonNull(action);
    this.executorService = requireNonNull(executorService);
    this.needToRetry = requireNonNull(needToRetry);
    this.exceptionalNeedToRetry = Objects.requireNonNullElseGet(exceptionalNeedToRetry, () -> t -> false);
  }

  @Override
  public void accept(final T data) {
    submitTask(data);
  }

  private void submitTask(final T data) {
    executorService.execute(() -> {
      try {
        final U result = action.apply(data);
        if (needToRetry.test(result)) {
          submitTask(data);
        }
      } catch (Throwable t) {
        if (exceptionalNeedToRetry.test(t)) {
          submitTask(data);
        }
      }
    });
  }

}
