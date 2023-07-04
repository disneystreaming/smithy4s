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

package smithy4s.decline.core

import smithy4s.Blob
import cats.Functor
import smithy4s.capability.Covariant
import com.monovore.decline.Argument
import cats.data.Validated.Valid
import smithy4s.{ByteArray, ConstraintError, Document, Schema}
import cats.implicits._
import cats.MonadError

import java.util.Base64

object commons {
  def toKebabCase(s: String): String =
    s.replaceAll("([A-Z])", "-$1").toLowerCase.drop(1)

  implicit def covariantAnyFunctor[F[_]](implicit
      ev: MonadError[F, ConstraintError]
  ): Covariant[F] =
    new Covariant[F] {
      def map[A, B](fa: F[A])(f: A => B): F[B] = Functor[F].map(fa)(f)
    }

  def parseJson[A](schema: Schema[A]): String => Either[String, A] = {
    val reader = smithy4s.json.Json.payloadCodecs.fromSchema(schema).reader
    s =>
      reader
        .decode(Blob(s))
        .leftMap(pe => pe.toString)
  }

  implicit val docArgument: Argument[Document] = {
    val parse = parseJson(Schema.document)

    Argument.from("json")(parse(_).toValidatedNel)
  }
  val byteArrayArgument: Argument[ByteArray] = {
    val decoder = Base64.getDecoder
    Argument.from("base64")(s => Valid(ByteArray(decoder.decode(s))))
  }
}

final case class RefinementFailed(message: String)
    extends Exception("Refinement failed: " + message)
