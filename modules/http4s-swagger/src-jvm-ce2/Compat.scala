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
import cats.effect._
import org.http4s.HttpRoutes
import org.http4s.Request
import org.http4s.Response
import org.http4s.StaticFile

private[smithy4s] object Compat {
  trait Package {
    private[smithy4s] type EffectCompat[F[_]] = cats.effect.Sync[F]
    private[smithy4s] val EffectCompat = cats.effect.Sync

    def docs[F[_]](
        hasId: HasId,
        blocker: Blocker,
        path: String = "docs"
    )(implicit
        F: Sync[F],
        CS: ContextShift[F]
    ): HttpRoutes[F] = {
      val docs = Docs[F](hasId, blocker, path)
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
        blocker: Blocker,
        path: String,
        swaggerUiPath: String = swaggerUiPath
    )(implicit
        F: Sync[F],
        CS: ContextShift[F]
    ): Docs[F] = {
      new Docs[F](hasId, path, swaggerUiPath) {

        override def staticResource(name: String, req: Option[Request[F]]) =
          StaticFile.fromResource(name, blocker, req)
      }
    }

  }
}
