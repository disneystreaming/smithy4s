---
sidebar_label: Error Stack Trace
title: Throwable Usage
---


By default, smithy4s generates error data types that extend `java.lang.Throwable` , For example the following spec:

```kotlin
@error("client")
structure BadRequest {
  @required
  reason: String
}

```

will generate following scala type:

```scala
case class BadRequest(reason: String) extends Throwable
```

It is however possible to annotate the error  definition with the `smithy4s.meta#noStackTrace` trait

```kotlin
@error("client")
@noStackTrace
structure BadRequest {
  @required
  reason: String
}
```
This will result in generated type extending `scala.util.control.NoStackTrace`

```scala
case class BadRequest(reason: String) extends scala.util.control.NoStackTrace
```