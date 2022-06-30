/*
 *  Copyright 2021 Disney Streaming
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
package dynamic

import Document._

import DummyIO._
import cats.syntax.all._

/**
  * These are toy interpreters that turn services into json-in/json-out
  * functions, and vice versa.
  *
  * Created for testing purposes.
  */
object JsonIOProtocol {

  def dummy[Alg[_[_, _, _, _, _]], Op[_, _, _, _, _]](
      service: Service.Provider[Alg, Op]
  ): Document => IO[Document] = {
    implicit val S: Service[Alg, Op] = service.service
    toJsonIO[Alg, Op](DummyService[IO].create[Alg, Op])
  }

  def redactingProxy[Alg[_[_, _, _, _, _]], Op[_, _, _, _, _]](
      jsonIO: Document => IO[Document],
      service: Service.Provider[Alg, Op]
  ): Document => IO[Document] = {
    implicit val S: Service[Alg, Op] = service.service
    toJsonIO[Alg, Op](fromJsonIO[Alg, Op](jsonIO)) andThen (_.map(redact))
  }

  def redact(document: Document): Document = document match {
    case DString("sensitive") => DString("*****")
    case DArray(array)        => DArray(array.map(redact))
    case DObject(map)         => DObject(map.fmap(redact))
    case other                => other
  }

  def fromJsonIO[Alg[_[_, _, _, _, _]], Op[_, _, _, _, _]](
      jsonIO: Document => IO[Document]
  )(implicit S: Service[Alg, Op]): Monadic[Alg, IO] = {
    val kleisliCache =
      fromLowLevel(jsonIO).precompute(S.endpoints.map(Kind5.existential(_)))
    val transfo = new Transformation[Op, GenLift[IO]#λ] {
      def apply[I, E, O, SI, SO](op: Op[I, E, O, SI, SO]): IO[O] = {
        val (input, ep) = S.endpoint(op)
        kleisliCache(ep).apply(input)
      }
    }
    S.transform[GenLift[IO]#λ](transfo)
  }

  def toJsonIO[Alg[_[_, _, _, _, _]], Op[_, _, _, _, _]](
      alg: Monadic[Alg, IO]
  )(implicit S: Service[Alg, Op]): Document => IO[Document] = {
    val transformation = S.asTransformation[GenLift[IO]#λ](alg)
    val jsonEndpoints =
      S.endpoints.map(ep => ep.name -> toLowLevel(transformation, ep)).toMap
    (d: Document) => {
      d match {
        case Document.DObject(m) if m.size == 1 =>
          val (method, payload) = m.head
          jsonEndpoints.get(method) match {
            case Some(jsonEndpoint) => jsonEndpoint(payload)
            case None               => IO.raiseError(NotFound)
          }
        case _ => IO.raiseError(NotFound)
      }
    }
  }

  private type KL[I, E, O, SI, SO] = I => IO[O]

  private def fromLowLevel[Op[_, _, _, _, _]](
      jsonIO: Document => IO[Document]
  ): Transformation[Endpoint[Op, *, *, *, *, *], KL] =
    new Transformation[Endpoint[Op, *, *, *, *, *], KL] {
      def apply[I, E, O, SI, SO](
          ep: Endpoint[Op, I, E, O, SI, SO]
      ): KL[I, E, O, SI, SO] = {
        implicit val encoderI: Document.Encoder[I] =
          Document.Encoder.fromSchema(ep.input)
        val decoderO: Document.Decoder[O] =
          Document.Decoder.fromSchema(ep.output)

        val decoderE: Document.Decoder[IO[Nothing]] =
          ep.errorable match {
            case Some(errorableE) =>
              Document.Decoder
                .fromSchema(errorableE.error)
                .map(e => IO.raiseError(errorableE.unliftError(e)))
            case None =>
              new Document.Decoder[IO[Nothing]] {
                def decode(
                    document: Document
                ): Either[smithy4s.http.PayloadError, IO[Nothing]] =
                  Right(
                    IO.raiseError(
                      smithy4s.http
                        .PayloadError(PayloadPath.root, "Nothing", "Nothing")
                    )
                  )
              }
          }
        implicit val decoderIOOnput = new Document.Decoder[IO[O]] {
          def decode(
              document: Document
          ): Either[smithy4s.http.PayloadError, IO[O]] = {
            document match {
              case Document.DObject(map) if (map.contains("error")) =>
                decoderE.decode(map("error"))
              case other => decoderO.decode(other).map(IO.pure(_))
            }
          }
        }

        (i: I) =>
          jsonIO(Document.obj(ep.name -> Document.encode(i)))
            .flatMap(_.decode[IO[O]].liftTo[IO].flatten)
      }
    }

  private def toLowLevel[Op[_, _, _, _, _], I, E, O, SI, SO](
      transformation: Transformation[Op, GenLift[IO]#λ],
      endpoint: Endpoint[Op, I, E, O, SI, SO]
  ): Document => IO[Document] = {
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
        input <- document.decode[I].liftTo[IO]
        op = endpoint.wrap(input)
        output <- transformation(op).map(encoderO.encode).recover {
          case endpoint.Error((_, e)) =>
            Document.obj("error" -> encoderE.encode(e))
        }
      } yield output
  }

  case object NotFound extends Throwable

}
