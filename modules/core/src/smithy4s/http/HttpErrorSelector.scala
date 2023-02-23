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
package http

import smithy.api.Error
import smithy4s.schema.SchemaAlt
import smithy.api.HttpError
import smithy4s.schema.CachedSchemaCompiler
import smithy4s.capability.Covariant
import smithy4s.kinds.PolyFunction
import smithy4s.kinds.Kind1

/**
  * Utility function to help find the decoder matching a certain discriminator
  * This is useful when deserializing on the client side of a request/response round trip.
  */
object HttpErrorSelector {

  /**
    * Given a vector of alternatives, and a schema compiler, selects the decoder
    * associated to the given discriminator.
    *
    * @param maybeErrorable: the Errorable instance associated to an operation
    * @param compiler: the compiler for a given decoder
    */
  def apply[F[_]: Covariant, E](
      maybeErrorable: Option[Errorable[E]],
      compiler: CachedSchemaCompiler[F]
  ): HttpDiscriminator => Option[F[E]] = maybeErrorable match {
    case None => _ => None
    case Some(errorable) =>
      new HttpErrorSelector[F, E](
        errorable.error.alternatives,
        compiler
      )
  }

  /**
    * Given a vector of alternatives, and a schema compiler, selects the decoder
    * associated to the given discriminator, and maps it so that it lifts
    * a throwable
    *
    * @param maybeErrorable: the Errorable instance associated to an operation
    * @param compiler: the compiler for a given decoder
    */
  def asThrowable[F[_]: Covariant, E](
      maybeErrorable: Option[Errorable[E]],
      compiler: CachedSchemaCompiler[F]
  ): HttpDiscriminator => Option[F[Throwable]] = maybeErrorable match {
    case None => _ => None
    case Some(errorable) =>
      new HttpErrorSelector[F, E](
        errorable.error.alternatives,
        compiler
      ).andThen(_.map(Covariant[F].map(_)(errorable.unliftError)))
  }

}

private[http] final class HttpErrorSelector[F[_]: Covariant, E](
    alts: Vector[SchemaAlt[E, _]],
    compiler: CachedSchemaCompiler[F]
) extends (HttpDiscriminator => Option[F[E]]) {

  type ConstF[A] = F[E]
  val cachedDecoders: PolyFunction[SchemaAlt[E, *], ConstF] =
    new PolyFunction[SchemaAlt[E, *], ConstF] {
      def apply[A](alt: SchemaAlt[E, A]): F[E] = {
        val schema = alt.instance
        // TODO: apply proper memoization of error instances/
        // In the line below, we create a new, ephemeral cache for the dynamic recompilation of the error schema.
        // This is because the "compile entity encoder" method can trigger a transformation of hints, which
        // lead to cache-miss and would lead to new entries in existing cache, effectively leading to a memory leak.
        val cache = compiler.createCache()
        val errorCodec: F[A] = compiler.fromSchema(schema, cache)
        Covariant[F].map[A, E](errorCodec)(alt.inject)
      }
    }.unsafeCacheBy(
      alts.map(Kind1.existential(_)),
      identity(_)
    )

  def apply(
      discriminator: HttpDiscriminator
  ): Option[F[E]] = {
    val alt = getPreciseAlternative(discriminator)
    alt.map(cachedDecoders(_))
  }

  private val byShapeId = alts
    .map { alt => alt.instance.shapeId -> alt }
    .toMap[ShapeId, SchemaAlt[E, _]]

  private val byName = alts
    .map(alt => alt.instance.shapeId.name -> alt)
    .toMap[String, SchemaAlt[E, _]]

  // build a map: status code to alternative
  // exclude all status code that are used on multiple alternative
  // in essence, it gives a `Map[Int, SchemaAlt[E, _]]` that's used
  // for the lookup
  private val byStatusCode: Int => Option[SchemaAlt[E, _]] = {
    val perStatusCode: Map[Int, SchemaAlt[E, _]] = alts
      .flatMap { alt =>
        alt.hints.get(HttpError).map { he => he.value -> alt }
      }
      .groupBy(_._1)
      .collect {
        // Discard alternative where another alternative has the same http status code
        case (status, allAlts) if allAlts.size == 1 => status -> allAlts.head._2
      }
      .toMap
    val errorForStatus: Int => Option[SchemaAlt[E, _]] = perStatusCode.get

    lazy val fallbackError: Int => Option[SchemaAlt[E, _]] = {
      // grab the alt that's annotated with the expected `Error` hint
      // only if there is only one
      def forErrorType(expected: Error): Option[SchemaAlt[E, _]] = {
        val matchingAlts = alts
          .flatMap { alt =>
            alt.hints
              .get(HttpError)
              .fold(
                alt.hints.get(Error).collect {
                  case e if e == expected => alt
                }
              )(_ => None)

          }
        if (matchingAlts.size == 1) matchingAlts.headOption else None
      }
      val clientAlt: Option[SchemaAlt[E, _]] = forErrorType(Error.CLIENT)
      val serverAlt: Option[SchemaAlt[E, _]] = forErrorType(Error.SERVER)

      { intStatus =>
        if (intStatus >= 400 && intStatus < 500) clientAlt
        else if (intStatus >= 500 && intStatus < 600) serverAlt
        else None
      }
    }

    inputStatus =>
      errorForStatus(inputStatus).orElse(fallbackError(inputStatus))
  }

  private[http] def getPreciseAlternative(
      discriminator: HttpDiscriminator
  ): Option[SchemaAlt[E, _]] = {
    import HttpErrorDiscriminator._
    discriminator match {
      case FullId(shapeId) => byShapeId.get(shapeId)
      case NameOnly(name)  => byName.get(name)
      case StatusCode(int) => byStatusCode(int)
    }
  }
}
