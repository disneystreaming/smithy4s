/*
 *  Copyright 2021-2022 Disney Streaming
 *
 *  Licensed under the Tomorrow Open Source Technology License, Version 1.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     https://disneystreaming.github.io/TOST-1.0.txt
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package smithy4s.guides

import smithy4s.guides.hello._
import cats.effect.*
import cats.implicits.*
import org.http4s.implicits.*
import org.http4s.ember.server.*
import org.http4s.*
import com.comcast.ip4s.*
import smithy4s.http4s.SimpleRestJsonBuilder
import org.http4s.server.Middleware
import org.http4s.headers.Origin
import org.typelevel.ci.CIString.apply
import org.typelevel.ci.CIString
import scala.concurrent.duration.Duration

object HelloWorldImpl extends HelloWorldService[IO] {
  def sayWorld(): IO[World] = World().pure[IO]
}

/**
  * This is an example of how you can configure CORS on a http4s router. The
  * source of truth for the configuration is the Smithy annotation on the
  * service. There is currently no support for CORS in Smithy4s because
  * the `@cors` annotation is a too restrictive.
  *
  * See: https://github.com/awslabs/smithy/issues/1396
  */
object Routes {
  import org.http4s.server.middleware.*

  val noMiddleware = (routes: HttpRoutes[IO]) => routes

  /**
    * We build a http4s CORS policy from the trait values.
    * In this example, we set a few fields like the `origin`, the `maxAge`, etc
    * but there is more information available in `smithy.api.Cors`.
    *
    * @param corsConfig instance of the trait with values from the specification
    */
  def buildHttp4sCors(corsConfig: smithy.api.Cors): CORSPolicy = {
    val configuredOrigin = Origin.parse(corsConfig.origin.value)
    configuredOrigin.swap.foreach { err =>
      // There are better approach for error handling than a println but
      // this will do for an example.
      println(
        s"Could not configure cors origin: ${err.getMessage()}"
      )
    }
    val configuredExposedHeaders =
      corsConfig.additionalExposedHeaders.toList.flatten
        .map(_.value)
        .toSet
        .map(CIString(_))
    val configuredAge =
      Duration(corsConfig.maxAge, scala.concurrent.duration.SECONDS)
    CORS.policy
      .withAllowOriginHeader {
        case Origin.Null => false
        case o =>
          configuredOrigin.toOption.exists { co => co == o }
      }
      .withExposeHeadersIn(configuredExposedHeaders)
      .withAllowCredentials(false)
      .withMaxAge(configuredAge)
  }

  // We use Smithy4s api to retrieve the trait (called `Hint` in Smithy4s)
  // We get an `Option` because service are not required to have this trait
  val corsTrait = HelloWorldServiceGen.hints.get[smithy.api.Cors]
  val corsMiddleWare = corsTrait
    .map { corsConfig => buildHttp4sCors(corsConfig).apply(_: HttpRoutes[IO]) }
    .getOrElse(noMiddleware)

  private val helloRoutes: Resource[IO, HttpRoutes[IO]] =
    SimpleRestJsonBuilder.build.routes(HelloWorldImpl).resource

  val all: Resource[IO, HttpRoutes[IO]] =
    helloRoutes.map(r => corsMiddleWare(r))
}

object Main extends IOApp.Simple {
  val run = Routes.all.flatMap { routes =>
    EmberServerBuilder
      .default[IO]
      .withPort(port"9000")
      .withHost(host"localhost")
      .withHttpApp(routes.orNotFound)
      .build
  }.useForever

}
