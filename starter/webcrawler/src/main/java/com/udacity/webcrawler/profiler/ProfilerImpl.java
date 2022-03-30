package com.udacity.webcrawler.profiler;

import com.google.inject.Module;
import com.udacity.webcrawler.parser.PageParser;
import org.jsoup.Connection;

import javax.inject.Inject;
import javax.swing.text.html.parser.Parser;
import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Clock;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import static java.time.format.DateTimeFormatter.RFC_1123_DATE_TIME;

/**
 * Concrete implementation of the {@link Profiler}.
 */
final class ProfilerImpl implements Profiler {

  private final Clock clock;
  private final ProfilingState state = new ProfilingState();
  private final ZonedDateTime startTime;
  private ProfilingMethodInterceptor handler;
  private Object proxy;

  @Inject
  ProfilerImpl(Clock clock) {
    this.clock = Objects.requireNonNull(clock);
    this.startTime = ZonedDateTime.now(clock);
  }

  @Override
  public <T> T wrap(Class<T> klass, T delegate) {
    Objects.requireNonNull(klass);

    // TODO: Use a dynamic proxy (java.lang.reflect.Proxy) to "wrap" the delegate in a
    //       ProfilingMethodInterceptor and return a dynamic proxy from this method.
    //       See https://docs.oracle.com/javase/10/docs/api/java/lang/reflect/Proxy.html.
    proxy = Proxy.newProxyInstance(delegate.getClass().getClassLoader(),
            new Class<?>[]{delegate.getClass().getInterfaces()[0]},
            new ProfilingMethodInterceptor(clock,delegate));
    //return klass.cast(proxy);



    return delegate;
  }

  @Override
  public void writeData(Path path) {
    // TODO: Write the ProfilingState data to the given file path. If a file already exists at that
    //       path, the new data should be appended to the existing file.

    Objects.requireNonNull(path);
    try (Writer bufferedWriter = Files.newBufferedWriter(path, StandardOpenOption.APPEND)) {
      writeData(bufferedWriter);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  @Override
  public void writeData(Writer writer) throws IOException {
    handler = (ProfilingMethodInterceptor) Proxy.getInvocationHandler(proxy);
    for(Map.Entry<Method,Duration> entry: handler.getProfileState().entrySet()){
      state.record(entry.getKey().getDeclaringClass(),entry.getKey(),entry.getValue());
    }
    writer.write("Run at " + RFC_1123_DATE_TIME.format(startTime));
    writer.write(System.lineSeparator());
    state.write(writer);
    writer.write(System.lineSeparator());
  }
}
