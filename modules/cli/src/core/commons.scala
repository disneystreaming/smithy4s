package smithy4s.cli.core

import cats.Functor
import smithy4s.capability.Covariant
import com.monovore.decline.Argument
import smithy4s.Schema
import smithy4s.Document
import cats.implicits._
import smithy4s.ConstraintError
import cats.MonadError

object commons {
  def toKebabCase(s: String): String = s.replaceAll("([A-Z])", "-$1").toLowerCase.drop(1)

  implicit def covariantAnyFunctor[F[_]](
    implicit ev: MonadError[F, ConstraintError]
  ): Covariant[F] =
    new Covariant[F] {
      def map[A, B](fa: F[A])(f: A => B): F[B] = Functor[F].map(fa)(f)
      def emap[A, B](fa: F[A])(f: A => Either[ConstraintError, B]): F[B] = fa.map(f).rethrow
    }

  def parseJson[A](schema: Schema[A]): String => Either[String, A] = {
    val capi = smithy4s.http.json.codecs()
    val codec = capi.compileCodec(schema)

    s =>
      capi
        .decodeFromByteArray(codec, s.getBytes())
        .leftMap(pe => pe.toString)
  }

  implicit val docArgument: Argument[Document] = {
    val parse = parseJson(Schema.document)

    Argument.from("json")(parse(_).toValidatedNel)
  }

}

final case class RefinementFailed(message: String)
  extends Exception("Refinement failed: " + message)
