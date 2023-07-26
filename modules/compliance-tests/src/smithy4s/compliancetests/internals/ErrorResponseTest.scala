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

package smithy4s.compliancetests
package internals

import smithy4s.Document
import smithy4s.schema.Schema
import smithy4s.Errorable
import cats.ApplicativeThrow
import cats.kernel.Eq
import cats.syntax.all._
import smithy4s.compliancetests.internals.eq.EqSchemaVisitor
import smithy4s.compliancetests.ComplianceTest.ComplianceResult

private[compliancetests] final case class ErrorResponseTest[A, E](
    schema: Schema[A],
    inject: A => E,
    dispatcher: E => Option[A],
    errorable: Errorable[E]
) {

  lazy val errorDecoder: Document.Decoder[A] =
    CanonicalSmithyDecoder.fromSchema(schema)
  implicit lazy val eq: Eq[A] = EqSchemaVisitor(schema)

  private def dispatchThrowable(t: Throwable): Option[A] = {
    errorable.liftError(t).flatMap(dispatcher(_))
  }

  def errorEq[F[_]: ApplicativeThrow]
      : (Document, Throwable) => F[ComplianceResult] = {

    (doc: Document, throwable: Throwable) =>
      errorDecoder
        .decode(doc)
        .map(inject)
        .map { e =>
          (dispatcher(e), dispatchThrowable(throwable)) match {
            case (Some(expected), Some(result)) =>
              assert.eql(result, expected)
            case _ =>
              assert.fail(
                s"Could not decode error response to known model: $throwable"
              )
          }
        }
        .liftTo[F]
  }
  def kleisliFy[F[_]: ApplicativeThrow]: Document => F[Throwable] = {
    (doc: Document) =>
      errorDecoder
        .decode(doc)
        .map(inject)
        .map(errorable.unliftError)
        .liftTo[F]
  }

}

private[compliancetests] object ErrorResponseTest {
  def from[E, A](
      errorAlt: smithy4s.schema.Alt[E, A],
      errorable: smithy4s.Errorable[E]
  ): ErrorResponseTest[A, E] =
    ErrorResponseTest(
      errorAlt.schema,
      errorAlt.inject,
      errorAlt.project,
      errorable
    )
}
