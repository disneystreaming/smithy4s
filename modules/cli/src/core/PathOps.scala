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

package smithy4s.cli.core

import cats.implicits._
import fs2.io.file.Files
import fs2.io.file.Path
import cats.effect.kernel.Async
import smithy4s.ByteArray

trait PathOps[F[_]] {
  def path(path: String): F[ByteArray]
}

object PathOps {
  def apply[F[_]](implicit F: PathOps[F]): PathOps[F] = F

  def instance[F[_]: Async](
    bufSize: Int = 4096
  ): PathOps[F] =
    path => {
      val input =
        path match {
          case "-" => fs2.io.stdin[F](bufSize)(Async[F])
          case _   => Files[F].readAll(Path(path))
        }

      input.compile.toVector.map(_.toArray).map(ByteArray(_))
    }

}
