package com.udacity.webcrawler.profiler;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/**
 * A method interceptor that checks whether {@link Method}s are annotated with the {@link Profiled}
 * annotation. If they are, the method interceptor records how long the method invocation took.
 */
final class ProfilingMethodInterceptor implements InvocationHandler {

  private final Clock clock;
  private final ProfilingState state;
  private final Object delegate;

  public ProfilingMethodInterceptor(Clock clock, ProfilingState state, Object delegate) {
    this.clock = Objects.requireNonNull(clock);
    this.state = Objects.requireNonNull(state);
    this.delegate = Objects.requireNonNull(delegate);
  }

  @Override
  public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
    Object res;
    Instant start = null;
    boolean isProfiled = method.getAnnotation(Profiled.class) != null;
    if (isProfiled) start = clock.instant();

    try {
      res = method.invoke(delegate, args);
    } catch (IllegalAccessException iae) {
      throw new RuntimeException(iae);
    } catch (InvocationTargetException ite) {
      throw ite.getTargetException();
    } finally {
      if (isProfiled) {
        state.record(this.delegate.getClass(), method, Duration.between(start, clock.instant()));
      }
    }

    return res;
  }
}
