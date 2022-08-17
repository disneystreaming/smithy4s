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
