package smithy4s.http4s

import cats.effect.Async
import smithy4s.http4s.kernel._

trait SimpleProtocolCodecs {

  def makeServerCodecs[F[_]: Async]: UnaryServerCodecs[F]
  def makeClientCodecs[F[_]: Async]: UnaryClientCodecs[F]

}
