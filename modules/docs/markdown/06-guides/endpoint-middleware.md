---
sidebar_label: Endpoint Specific Middleware
title: Endpoint Specific Middleware
---

It used to be the case that any middleware implemented for smithy4s services would have to operate at the http4s level, without any knowledge of smithy4s or access to the constructs to utilizes.

As of version `0.17.x` of smithy4s, we have changed this by providing a new mechanism to build and provide middleware. This mechanism is aware of the smithy4s service and endpoints that are derived from your smithy specifications. As such, this unlocks the possibility to build middleware that utilizes and is compliant to the traits and shapes of your smithy specification.

In this guide, we will show how you can implement a smithy4s middleware that is aware of the authentication traits in your specification and is able to implement authenticate on an endpoint-by-endpoint basis. This is useful if you have different or no authentication on one or more endpoints.

## EndpointSpecificMiddleware

`EndpointSpecificMiddleware` is the interface that we have provided for implementing middleware. For some use cases, you will need to use the full interface. However, for this guide and for many uses cases, you will be able to rely on the simpler interface called `EndpointSpecificMiddlewareSpec.Simple`. This interface requires a single method which looks as follows:

```scala
def prepareUsingHints(
        serviceHints: Hints,
        endpointHints: Hints
    ): HttpApp[F] => HttpApp[F]
```

This means that given the hints for the service and a specific endpoint, our implementation will provide a transformation of an `HttpApp`. If you are not familiar with `Hints`, they are the smithy4s construct that represents Smithy Traits. They are called hints to avoid naming conflicts and confusion with Scala `trait`s.

## Smithy Spec

Let's look at the smithy specification that we will use for this guide. First, let's define the service.

```kotlin
$version: "2"

namespace smithy4s.guides.auth

use alloy#simpleRestJson

@simpleRestJson
@httpBearerAuth
service HelloWorldAuthService {
  version: "1.0.0",
  operations: [SayWorld, HealthCheck]
  errors: [NotAuthorizedError]
}
```

Here we defined a service that has two operations, `SayWorld` and `HealthCheck`. We defined it such that any of these operations may return an `NotAuthorizedError`. Finally, we annotated the service with the `@httpBearerAuth` [trait](https://smithy.io/2.0/spec/authentication-traits.html#httpbearerauth-trait) to indicate that the service supports authentication via a bearer token. If you are using a different authentication scheme, you can still follow this guide and adapt it for your needs. You can find a full list of smithy-provided schemes [here](https://smithy.io/2.0/spec/authentication-traits.html). If none of the provided traits suit your use case, you can always create a custom trait too.

Next, let's define our first operation, `SayWorld`:

```kotlin
@readonly
@http(method: "GET", uri: "/hello", code: 200)
operation SayWorld {
  output: World
}

structure World {
  message: String = "World !"
}
```

There is nothing authentication-specific defined with this operation, this means that the operation inherits the service-defined authentication scheme (`httpBearerAuth` in this case). Let's contrast this with the `HealthCheck` operation:

```kotlin
@readonly
@http(method: "GET", uri: "/health", code: 200)
@auth([])
operation HealthCheck {
  output := {
    @required
    message: String
  }
}
```

Notice that on this operation we have added the `@auth([])` trait with an empty array. This means that there is no authentication required for this endpoint. In other words, although the service defines an authentication scheme of `httpBearerAuth`, that scheme will not apply to this endpoint.

Finally, let's define the `NotAuthorizedError` that will be returned when an authentication token is missing or invalid.

```kotlin
@error("client")
@httpError(401)
structure NotAuthorizedError {
  @required
  message: String
}
```

There is nothing authentication specific about this error, this is a standard smithy http error that will have a 401 status code when returned.

If you want to see the full smithy model we defined above, you can do so [here](https://github.com/disneystreaming/smithy4s/blob/main/modules/guides/smithy/auth.smithy).

## Server-side Middleware

To see the **full code** example of what we walk through below, go [here](https://github.com/disneystreaming/smithy4s/tree/main/modules/guides/src/smithy4s/guides/Auth.scala).

We will create a server-side middleware that implements the authentication as defined in the smithy spec above. Let's start by creating a few classes that we will use in our middleware.

```scala mdoc:invisible
import smithy4s.guides.auth._
import cats.effect._
import cats.implicits._
import org.http4s.implicits._
import org.http4s._
import smithy4s.http4s.SimpleRestJsonBuilder
import smithy4s._
import org.http4s.headers.Authorization
import smithy4s.http4s.EndpointSpecificMiddleware
```

#### AuthChecker

```scala mdoc:silent
case class ApiToken(value: String)

trait AuthChecker {
  def isAuthorized(token: ApiToken): IO[Boolean]
}

object AuthChecker extends AuthChecker {
  def isAuthorized(token: ApiToken): IO[Boolean] = {
    IO.pure(
      token.value.nonEmpty
    ) // put your logic here, currently just makes sure the token is not empty
  }
}
```

This is a simple class that we will use to check the validity of a given token. This will be more complex in your own service, but we are keeping it simple here since it is out of the scope of this article and implementations will vary widely depending on your specific application.

#### The Inner Middleware Implementation

This function is what is called once we have made sure that the middleware is applicable for a given endpoint. We will show in the next step how to tell if the middleware is applicable or not. For now though, we will just focus on what the middleware does once we know that it needs to be applied to a given endpoint.

```scala mdoc:silent
def middleware(
    authChecker: AuthChecker // 1
): HttpApp[IO] => HttpApp[IO] = { inputApp => // 2
  HttpApp[IO] { request => // 3
    val maybeKey = request.headers // 4
      .get[`Authorization`]
      .collect {
        case Authorization(
              Credentials.Token(AuthScheme.Bearer, value)
            ) =>
          value
      }
      .map { ApiToken.apply }

    val isAuthorized = maybeKey
      .map { key =>
        authChecker.isAuthorized(key) // 5
      }
      .getOrElse(IO.pure(false))

    isAuthorized.ifM(
      ifTrue = inputApp(request), // 6
      ifFalse = IO.raiseError(new NotAuthorizedError("Not authorized!")) // 7
    )
  }
}
```

Let's break down what we did above step by step. The step numbers below correspond to the comment numbers above.

1. Pass an instance of `AuthChecker` that we can use to verify auth tokens are valid in this middleware
2. `inputApp` is the `HttpApp[IO]` that we are transforming in this middleware.
3. Here we create a new HttpApp, the one that we will be returning from this function we are creating.
4. Here we extract the value of the `Authorization` header, if it is present.
5. If the header had a value, we now send that value into the `AuthChecker` to see if it is valid.
6. If the token was found to be valid, we pass the request into the `inputApp` from step 2 in order to get a response.
7. If the header was found to be invalid, we return the `NotAuthorizedError` that we defined in our smithy file above.

#### EndpointSpecificMiddleware.Simple

Next, let's create our middleware by implementing the `EndpointSpecificMiddleware.Simple` interface we discussed above.

```scala mdoc:silent
object AuthMiddleware {
  def apply(
      authChecker: AuthChecker // 1
  ): EndpointSpecificMiddleware[IO] =
    new EndpointSpecificMiddleware.Simple[IO] {
      private val mid: HttpApp[IO] => HttpApp[IO] = middleware(authChecker) // 2
      def prepareUsingHints(
          serviceHints: Hints,
          endpointHints: Hints
      ): HttpApp[IO] => HttpApp[IO] = {
        serviceHints.get[smithy.api.HttpBearerAuth] match { // 3
          case Some(_) =>
            endpointHints.get[smithy.api.Auth] match { // 4
              case Some(auths) if auths.value.isEmpty => identity // 5
              case _                                  => mid // 6
            }
          case None => identity
        }
      }
    }
}
```

1. Pass in an instance of `AuthChecker` for the middleware to use. This is how the middleware will know if a given token is valid or not.
2. This is the function that we defined in the step above.
3. Check and see if the service at hand does in fact have the `httpBearerAuth` trait on it. If it doesn't, then we will not do our auth checks. If it does, then we will proceed.
4. Here we are getting the `@auth` trait from the operation (endpoint in smithy4s lingo). We need to check for this trait because of step 5.
5. Here we are checking that IF the auth trait is on this endpoint AND the auth trait contains an empty array THEN we are performing NO authentication checks. This is how we handle the `@auth([])` trait that is present on the `HealthCheck` operation we defined above.
6. IF the auth trait is NOT present on the operation, OR it is present AND it contains one or more authentication schemes, we apply the middleware.

#### Using the Middleware

From here, we can pass our middleware into our `SimpleRestJsonBuilder` as follows:

```scala mdoc:silent
object HelloWorldAuthImpl extends HelloWorldAuthService[IO] {
  def sayWorld(): IO[World] = World().pure[IO]
  def healthCheck(): IO[HealthCheckOutput] = HealthCheckOutput("Okay!").pure[IO]
}

val routes = SimpleRestJsonBuilder
      .routes(HelloWorldAuthImpl)
      .middleware(AuthMiddleware(AuthChecker))
      .resource
```

And that's it. Now we have a middleware that will apply an authentication check on incoming requests whenever relevant, as defined in our smithy file.

## Client-side Middleware

To see the **full code** example of what we walk through below, go [here](https://github.com/disneystreaming/smithy4s/tree/main/modules/guides/src/smithy4s/guides/AuthClient.scala).

It is possible that you have a client where you want to apply a similar type of middleware that alters some part of a request depending on the endpoint being targeted. In this part of the guide, we will show how you can do this for a client using the same smithy specification we defined above. We will make it so our authentication token is only sent if we are targeting an endpoint which requires it.

#### EndpointSpecificMiddleware.Simple

The interface that we define for this middleware is going to look very similar to the one we defined above. This makes sense because this middleware is effectively the dual of the middleware above.

```scala mdoc:silent
object Middleware {

  private def middleware(bearerToken: String): HttpApp[IO] => HttpApp[IO] = { // 1
    inputApp =>
      HttpApp[IO] { request =>
        val newRequest = request.withHeaders( // 2
          Authorization(Credentials.Token(AuthScheme.Bearer, bearerToken))
        )

        inputApp(newRequest)
      }
  }

  def apply(bearerToken: String): EndpointSpecificMiddleware[IO] = // 3
    new EndpointSpecificMiddleware.Simple[IO] {
      private val mid = middleware(bearerToken)
      def prepareUsingHints(
          serviceHints: Hints,
          endpointHints: Hints
      ): HttpApp[IO] => HttpApp[IO] = {
        serviceHints.get[smithy.api.HttpBearerAuth] match {
          case Some(_) =>
            endpointHints.get[smithy.api.Auth] match {
              case Some(auths) if auths.value.isEmpty => identity
              case _                                  => mid
            }
          case None => identity
        }
      }
    }

}
```

1. Here we are creating an inner middleware function, just like we did above. The only difference is that this time we are adding a value to the request instead of extracting one from it.
2. Add the `Authorization` header to the request and pass it to the `inputApp` that we are transforming in this middleware.
3. This function is actually the *exact same* as the function for the middleware we implemented above. The only difference is that this apply method accepts a `bearerToken` as a parameter. This is the token that we will add into the `Authorization` header when applicable.

#### SimpleRestJsonBuilder

```scala mdoc:invisible
import org.http4s.client._
```

As above, we now just need to wire our middleware into our actual implementation. Here we are constructing a client and specifying the middleware we just defined.

```scala mdoc:silent
def apply(http4sClient: Client[IO]): Resource[IO, HelloWorldAuthService[IO]] =
    SimpleRestJsonBuilder(HelloWorldAuthService)
      .client(http4sClient)
      .uri(Uri.unsafeFromString("http://localhost:9000"))
      .middleware(Middleware("my-token")) // creating our middleware here
      .resource
```

## Conclusion

Once again, if you want to see the **full code** examples of the above, you can find them [here](https://github.com/disneystreaming/smithy4s/tree/main/modules/guides/src/smithy4s/guides/).

Hopefully this guide gives you a good idea of how you can create a middleware that takes your smithy specification into account. This guide shows a very simple use case of what is possible with a middleware like this. If you have a more advanced use case, you can use this guide as a reference and as always you can reach out to us for insight or help.
