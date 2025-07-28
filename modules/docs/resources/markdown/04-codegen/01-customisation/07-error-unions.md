---
sidebar_label: Error Unions
title: Error Unions representation
---

By default, smithy4s renders service operations errors as ADTs. For example the following spec:

```kotlin
operation Operation {
  input: Unit,
  output: Unit,
  errors: [BadRequest, InternalServerError]
}

@error("client")
structure BadRequest {
  @required
  reason: String
}

@error("server")
structure InternalServerError {
  @required
  stackTrace: String
}
```

will generate following scala types (simplified for brevity):

```scala
case class BadRequest(reason: String) extends Throwable
case class InternalServerError(stackTrace: String) extends Throwable

sealed trait OperationError
object OperationError extends ShapeTag.Companion[OperationError] {
  case class BadRequestCase(badRequest: BadRequest) extends OperationError
  case class InternalServerErrorCase(internalServerError: InternalServerError) extends OperationError
}
```

For Scala 3 users that would like to use native union representation for error types, smithy4s exposes a metadata flag `smithy4sErrorsAsScala3Unions` (defaults to `false`).

After adding: 
```
metadata smithy4sErrorsAsScala3Unions = true
```

to any of the smithy files used for code generation, the following representation will be rendered instead:

```scala
case class BadRequest(reason: String) extends Throwable
case class InternalServerError(stackTrace: String) extends Throwable

type OperationError = BadRequest | InternalServerError
```
