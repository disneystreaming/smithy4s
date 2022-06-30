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

package smithy4s
package http4s.swagger

import cats.data.OptionT
import cats.data.NonEmptyList
import cats.effect.Sync
import org.http4s.HttpRoutes
import org.http4s.Request
import org.http4s.Response
import org.http4s.StaticFile

private[smithy4s] object Compat {
  trait Package {
    private[smithy4s] type EffectCompat[F[_]] = cats.effect.Concurrent[F]
    private[smithy4s] val EffectCompat = cats.effect.Concurrent

    def docs[F[_]](
        hasId: HasId,
        path: String = "docs"
    )(implicit
        F: Sync[F]
    ): HttpRoutes[F] = {
      multipleDocs(hasId)(path)
    }

    def multipleDocs[F[_]](
        id: HasId,
        rest: HasId*
    )(path: String)(implicit
        F: Sync[F]
    ): HttpRoutes[F] = {
      val docs = Docs.multiple[F](path)(id, rest: _*)
      docs.routes
    }
  }

  trait DocsClass[F[_]] {
    def staticResource(
        name: String,
        req: Option[Request[F]]
    ): OptionT[F, Response[F]]
  }

  trait DocsCompanion extends SwaggerUiInit {
    def apply[F[_]](
        hasId: HasId,
        path: String,
        swaggerUiPath: String = swaggerUiPath
    )(implicit
        F: Sync[F]
    ): Docs[F] = {
      multiple[F](path, swaggerUiPath)(hasId)
    }

    def multiple[F[_]](
        path: String,
        swaggerUiPath: String = swaggerUiPath
    )(id: HasId, rest: HasId*)(implicit
        F: Sync[F]
    ): Docs[F] = {
      new Docs[F](NonEmptyList(id, rest.toList), path, swaggerUiPath) {
        override def staticResource(name: String, req: Option[Request[F]]) = {
          StaticFile.fromResource(name, req)
        }
      }
    }
  }
}
