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
package tests

import Document._

import cats.syntax.all._
import cats.MonadThrow
import smithy4s.kinds._

/**
  * These are toy interpreters that turn services into json-in/json-out
  * functions, and vice versa.
  *
  * Created for testing purposes.
  */
class JsonProtocolF[F[_]](implicit F: MonadThrow[F]) {

  def dummy[Alg[_[_, _, _, _, _]]](
      service: Service[Alg]
  ): Document => F[Document] = {
    implicit val S: Service[Alg] = service
    toJsonF[Alg](DummyService[F].create[Alg])
  }

  def redactingProxy[Alg[_[_, _, _, _, _]]](
      jsonF: Document => F[Document],
      service: Service[Alg]
  ): Document => F[Document] = {
    implicit val S: Service[Alg] = service.service
    toJsonF[Alg](fromJsonF[Alg](jsonF)) andThen (_.map(redact))
  }

  def redact(document: Document): Document = document match {
    case DString("sensitive") => DString("*****")
    case DArray(array)        => DArray(array.map(redact))
    case DObject(map)         => DObject(map.fmap(redact))
    case other                => other
  }

  def fromJsonF[Alg[_[_, _, _, _, _]]](
      jsonF: Document => F[Document]
  )(implicit S: Service[Alg]): S.Impl[F] = fromLowLevel(S)(jsonF)

  def toJsonF[Alg[_[_, _, _, _, _]]](
      alg: FunctorAlgebra[Alg, F]
  )(implicit S: Service[Alg]): Document => F[Document] = {
    val transformation = S.toPolyFunction[Kind1[F]#toKind5](alg)
    val jsonEndpoints =
      S.endpoints.map(ep => ep.name -> toLowLevel(transformation, ep)).toMap
    (d: Document) => {
      d match {
        case Document.DObject(m) if m.size == 1 =>
          val (method, payload) = m.head
          jsonEndpoints.get(method) match {
            case Some(jsonEndpoint) => jsonEndpoint(payload)
            case None               => F.raiseError(NotFound)
          }
        case _ => F.raiseError(NotFound)
      }
    }
  }

  private def fromLowLevel[Alg[_[_, _, _, _, _]]](service: Service[Alg])(
      jsonF: Document => F[Document]
  ): service.Impl[F] = service.impl {
    new service.FunctorEndpointCompiler[F] {
      def apply[I, E, O, SI, SO](
          ep: service.Endpoint[I, E, O, SI, SO]
      ): I => F[O] = {
        implicit val encoderI: Document.Encoder[I] =
          Document.Encoder.fromSchema(ep.input)
        val decoderO: Document.Decoder[O] =
          Document.Decoder.fromSchema(ep.output)

        val decoderE: Document.Decoder[F[Nothing]] =
          ep.errorable match {
            case Some(errorableE) =>
              Document.Decoder
                .fromSchema(errorableE.error)
                .map(e => F.raiseError(errorableE.unliftError(e)))
            case None =>
              new Document.Decoder[F[Nothing]] {
                def decode(
                    document: Document
                ): Either[smithy4s.http.PayloadError, F[Nothing]] =
                  Right(
                    F.raiseError(
                      smithy4s.http
                        .PayloadError(PayloadPath.root, "Nothing", "Nothing")
                    )
                  )
              }
          }
        implicit val decoderFoutput = new Document.Decoder[F[O]] {
          def decode(
              document: Document
          ): Either[smithy4s.http.PayloadError, F[O]] = {
            document match {
              case Document.DObject(map) if (map.contains("error")) =>
                decoderE.decode(map("error")).map(_.asInstanceOf[F[O]])
              case other => decoderO.decode(other).map(F.pure(_))
            }
          }
        }

        (i: I) =>
          jsonF(Document.obj(ep.name -> Document.encode(i)))
            .flatMap(_.decode[F[O]].liftTo[F].flatten)
      }
    }
  }

  private def toLowLevel[Op[_, _, _, _, _], I, E, O, SI, SO](
      polyFunction: PolyFunction5[Op, Kind1[F]#toKind5],
      endpoint: Endpoint[Op, I, E, O, SI, SO]
  ): Document => F[Document] = {
    implicit val decoderI = Document.Decoder.fromSchema(endpoint.input)
    implicit val encoderO = Document.Encoder.fromSchema(endpoint.output)
    implicit val encoderE: Document.Encoder[E] =
      endpoint.errorable match {
        case Some(errorableE) =>
          Document.Encoder.fromSchema(errorableE.error)
        case None =>
          new Document.Encoder[E] {
            def encode(e: E): Document = Document.DNull
          }
      }
    (document: Document) =>
      for {
        input <- document.decode[I].liftTo[F]
        op = endpoint.wrap(input)
        output <- (polyFunction(op): F[O]).map(encoderO.encode).recover {
          case endpoint.Error((_, e)) =>
            Document.obj("error" -> encoderE.encode(e))
        }
      } yield output
  }

  case object NotFound extends Throwable

}
