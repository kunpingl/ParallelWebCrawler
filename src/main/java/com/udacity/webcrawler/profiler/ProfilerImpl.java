package com.udacity.webcrawler.profiler;

import javax.inject.Inject;
import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Clock;
import java.time.ZonedDateTime;
import java.util.Objects;

import static java.time.format.DateTimeFormatter.RFC_1123_DATE_TIME;

/** Concrete implementation of the {@link Profiler}. */
final class ProfilerImpl implements Profiler {

  private final Clock clock;
  private final ProfilingState state = new ProfilingState();
  private final ZonedDateTime startTime;

  @Inject
  ProfilerImpl(Clock clock) {
    this.clock = Objects.requireNonNull(clock);
    this.startTime = ZonedDateTime.now(clock);
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T> T wrap(Class<T> klass, T delegate) {
    Objects.requireNonNull(klass);

    Method[] methods = klass.getDeclaredMethods();
    for (Method method : methods) {
      if (method.getAnnotation(Profiled.class) != null) {
        ProfilingMethodInterceptor profilingMethodInterceptor =
            new ProfilingMethodInterceptor(this.clock, this.state, delegate);
        return (T)
            Proxy.newProxyInstance(
                delegate.getClass().getClassLoader(),
                new Class<?>[] {klass},
                profilingMethodInterceptor);
      }
    }
    throw new IllegalArgumentException();
  }

  @Override
  public void writeData(Path path) {
    Objects.requireNonNull(path);
    try (Writer writer =
        Files.newBufferedWriter(
            path, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
      writeData(writer);
    } catch (IOException ioe) {
      ioe.printStackTrace();
    }
  }

  @Override
  public void writeData(Writer writer) throws IOException {
    writer.write("Run at " + RFC_1123_DATE_TIME.format(startTime));
    writer.write(System.lineSeparator());
    state.write(writer);
    writer.write(System.lineSeparator());
  }
}
