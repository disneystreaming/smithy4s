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

package smithy4s.http4s.kernel

import org.http4s.Response
import org.http4s.Status
import smithy4s.Errorable
import smithy4s.capability.EncoderK
import smithy4s.http.HttpStatusCode
import smithy4s.schema.Alt
import smithy4s.schema.CachedSchemaCompiler
import smithy4s.schema.Schema

trait ResponseEncoder[F[_], A] {
  def addToResponse(response: Response[F], a: A): Response[F]
}

object ResponseEncoder {

  def empty[F[_], A]: ResponseEncoder[F, A] = new ResponseEncoder[F, A] {
    def addToResponse(response: Response[F], a: A): Response[F] = response
  }

  def combine[F[_], A](
      left: ResponseEncoder[F, A],
      right: ResponseEncoder[F, A]
  ): ResponseEncoder[F, A] = new ResponseEncoder[F, A] {
    def addToResponse(response: Response[F], a: A): Response[F] =
      right.addToResponse(left.addToResponse(response, a), a)
  }

  def forError[F[_], E](
      errorTypeHeader: String,
      maybeErrorable: Option[Errorable[E]],
      encoderCompiler: CachedSchemaCompiler[ResponseEncoder[F, *]]
  ): ResponseEncoder[F, E] = maybeErrorable match {
    case Some(errorable) =>
      forErrorAux(errorTypeHeader, errorable, encoderCompiler)
    case None => noop[F, E]
  }

  private def forErrorAux[F[_], E](
      errorTypeHeader: String,
      errorable: Errorable[E],
      encoderCompiler: CachedSchemaCompiler[ResponseEncoder[F, *]]
  ): ResponseEncoder[F, E] = {
    val errorUnionSchema = errorable.error
    val dispatcher =
      Alt.Dispatcher(errorUnionSchema.alternatives, errorUnionSchema.dispatch)
    val precompiler = new Alt.Precompiler[Schema, ResponseEncoder[F, *]] {
      def apply[Err](
          label: String,
          errorSchema: Schema[Err]
      ): ResponseEncoder[F, Err] = new ResponseEncoder[F, Err] {
        val errorEncoder =
          encoderCompiler.fromSchema(errorSchema, encoderCompiler.createCache())
        def addToResponse(response: Response[F], err: Err): Response[F] = {
          val errorCode =
            HttpStatusCode.fromSchema(errorSchema).code(err, 500)
          val status =
            Status.fromInt(errorCode).getOrElse(Status.InternalServerError)
          val encodedResponse = errorEncoder.addToResponse(response, err)
          encodedResponse
            .withStatus(status)
            .withHeaders(encodedResponse.headers.put(errorTypeHeader -> label))
        }
      }
    }
    dispatcher.compile(precompiler)
  }

  def noop[F[_], A]: ResponseEncoder[F, A] = new ResponseEncoder[F, A] {
    def addToResponse(response: Response[F], a: A): Response[F] = response
  }

  // format: off
  implicit def responseEncoderEncoderK[F[_]]: EncoderK[ResponseEncoder[F, *], Response[F] => Response[F]] =
    new EncoderK[ResponseEncoder[F, *], Response[F] => Response[F]] {
      def apply[A](fa: ResponseEncoder[F, A], a: A): Response[F] => Response[F] = fa.addToResponse(_, a)
      def absorb[A](f: A => (Response[F] => Response[F])): ResponseEncoder[F, A] = new ResponseEncoder[F, A] {
        def addToResponse(response: Response[F], a: A): Response[F] =
          f(a)(response)
      }
    }
  // format: on

}
