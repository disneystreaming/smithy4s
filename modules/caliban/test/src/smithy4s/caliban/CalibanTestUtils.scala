package smithy4s.caliban

import caliban.RootResolver
import caliban.interop.cats.implicits._
import caliban.schema.Schema
import cats.effect.IO
import io.circe.Json
import io.circe.syntax._

object CalibanTestUtils {
  private implicit val rt: zio.Runtime[Any] = zio.Runtime.default

  def testQueryResult[A](api: A, q: String)(implicit
      aSchema: Schema[Any, A]
  ): IO[Json] =
    caliban
      .graphQL(RootResolver(api))
      .interpreterAsync[IO]
      .flatMap(_.executeAsync[IO](q))
      .map(_.data.asJson)

  def testQueryResultWithSchema[A: smithy4s.Schema](
      api: A,
      q: String
  ): IO[Json] =
    testQueryResult(api, q)(
      implicitly[smithy4s.Schema[A]].compile(CalibanSchemaVisitor)
    )
}
