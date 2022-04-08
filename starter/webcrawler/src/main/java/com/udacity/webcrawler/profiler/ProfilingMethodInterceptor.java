package com.udacity.webcrawler.profiler;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * A method interceptor that checks whether {@link Method}s are annotated with the {@link Profiled}
 * annotation. If they are, the method interceptor records how long the method invocation took.
 */
final class ProfilingMethodInterceptor implements InvocationHandler {

  private final Clock clock;
  private final Object delegate;
  private static Method hashCodeMethod;
  private static Method equalsMethod;
  private static Method toStringMethod;
  private static Map<Method, Duration> data = new LinkedHashMap<>();

  static {
    try {
      hashCodeMethod = Objects.class.getMethod("hashCode", Object.class);
      equalsMethod = Objects.class.getMethod("equals", Object.class, Object.class);
      toStringMethod = Objects.class.getMethod("toString", Object.class);
    }catch (NoSuchMethodException e){
      throw  new NoSuchMethodError(e.getMessage());
    }
  }

  // TODO: You will need to add more instance fields and constructor arguments to this class.
  ProfilingMethodInterceptor(Clock clock,Object delegate) {

    this.clock = Objects.requireNonNull(clock);
    this.delegate = Objects.requireNonNull(delegate);
  }

  @Override
  public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
    // TODO: This method interceptor should inspect the called method to see if it is a profiled
    //       method. For profiled methods, the interceptor should record the start time, then
    //       invoke the method using the object that is being profiled. Finally, for profiled
    //       methods, the interceptor should record how long the method call took, using the
    //       ProfilingState methods.


    if (method.getAnnotation(Profiled.class) != null) {


      Instant startTime = clock.instant();

      try {
        Object result = method.invoke(delegate, args);
        Duration elapse = Duration.between(startTime, clock.instant());
        data.put(method, elapse);
        return result;
      } catch (Exception e) {
          throw new Exception();
      }



    }

    else if (method.getName().equals(hashCodeMethod.getName())) {
      return proxyHashcode(delegate);
    }else if (method.getName().equals(equalsMethod.getName())) {
      return proxyEquals(proxy,args);
    }else if(method.getName().equals(toStringMethod.getName())) {
      return proxyToString(delegate);
    }

    Method[] methods = method.getDeclaringClass().getDeclaredMethods();

    int check = methods.length;
    if(check != 0) {
      for (Method method1 : methods) {
        if (method1.getAnnotation(Profiled.class) != null) {
          check = check - 1;
        }
      }
      if (check == methods.length) {
        throw  new IllegalArgumentException("Class has No methods annotated with Profiled");
      }
    }else{
      throw new IllegalArgumentException("Class has No methods annotated with Profiled");
    }



    //System.out.println("The invokation got here 3  method name: " + method.getName());
    //System.out.println("The invokation got here 3 method decl class : " + method.getDeclaringClass().getInterfaces()[0].getDeclaredMethods());
    //System.out.println("The invokation got here 3  proxy class: " + proxy.getClass());
    //System.out.println("The invokation got here 3 interface is annotated : " + method.getDeclaringClass().getMethods());

    return null;
  }
  public Map<Method,Duration> getProfileState(){return data;}

  protected Integer proxyHashcode(Object delegate){
    return System.identityHashCode(delegate);
  }
  protected Object proxyEquals(Object proxy,Object other){
    if(proxy !=null && other !=null) {
      return proxy.equals(other);
    }
    return false;
  }
  protected String proxyToString(Object delegate){
    return delegate.toString();
  }

}
