---
sidebar_label: Stubbed implementations
title: Stubbed implementations
---

For various reasons, such as testing/mocking, you may want to instantiate a stub implementation of generated service interfaces. Smithy4s makes it easy, by generating a `Default` class in the companion object of each service.

This class has a constructor parameter that requires a value. This value is what is returned when invoking any of the methods

```scala mdoc:silent
import smithy4s.hello._
import cats.effect._
val stubbedHelloWorld: HelloWorldService[IO] = new HelloWorldService.Default[IO](IO.stub)
```

Obviously, the generated methods can be overridden.

```scala mdoc:silent
import smithy4s.hello._
import cats.effect._
val overriddenHelloWorld: HelloWorldService[IO] = new HelloWorldService.Default[IO](IO.stub){
  override def hello(name: String, town: Option[String]): IO[Greeting] = IO.pure {
    Greeting(s"Hello $name!")
  }
}
```

**NB** it is important to ascribe the type of the stubbed instance when using this feature.
