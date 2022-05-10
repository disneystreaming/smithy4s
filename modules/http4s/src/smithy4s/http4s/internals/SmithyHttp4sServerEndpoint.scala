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
package http4s
package internals

import cats.data.Kleisli
import cats.syntax.all._
import org.http4s.EntityEncoder
import org.http4s.Header
import org.http4s.Headers
import org.http4s.Message
import org.http4s.Method
import org.http4s.Request
import org.http4s.Response
import org.http4s.Status
import org.typelevel.ci.CIString
import smithy4s.http.Metadata
import smithy4s.http._
import smithy4s.schema.Alt

/**
  * A construct that encapsulates a smithy4s endpoint, and exposes
  * http4s specific semantics.
  */
private[smithy4s] trait SmithyHttp4sServerEndpoint[F[_]] {
  def method: org.http4s.Method
  def matches(path: Array[String]): Option[PathParams]
  def run(pathParams: PathParams, request: Request[F]): F[Response[F]]

  def matchTap(
      path: Array[String]
  ): Option[(SmithyHttp4sServerEndpoint[F], PathParams)] =
    matches(path).map(this -> _)
}

private[smithy4s] object SmithyHttp4sServerEndpoint {

  def apply[F[_]: EffectCompat, Op[_, _, _, _, _], I, E, O, SI, SO](
      impl: Interpreter[Op, F],
      endpoint: Endpoint[Op, I, E, O, SI, SO],
      codecs: EntityCompiler[F],
      errorTransformation: PartialFunction[Throwable, F[Throwable]]
  ): Option[SmithyHttp4sServerEndpoint[F]] =
    HttpEndpoint.cast(endpoint).map { httpEndpoint =>
      new SmithyHttp4sServerEndpointImpl[F, Op, I, E, O, SI, SO](
        impl,
        endpoint,
        httpEndpoint,
        codecs,
        errorTransformation
      )
    }

}

// format: off
private[smithy4s] class SmithyHttp4sServerEndpointImpl[F[_], Op[_, _, _, _, _], I, E, O, SI, SO](
    impl: Interpreter[Op, F],
    endpoint: Endpoint[Op, I, E, O, SI, SO],
    httpEndpoint: HttpEndpoint[I],
    codecs: EntityCompiler[F],
    errorTransformation: PartialFunction[Throwable, F[Throwable]]
)(implicit F: EffectCompat[F]) extends SmithyHttp4sServerEndpoint[F] {
// format: on

  type ==>[A, B] = Kleisli[F, A, B]

  val method: Method = toHttp4sMethod(httpEndpoint.method)

  def matches(path: Array[String]): Option[PathParams] = {
    httpEndpoint.matches(path)
  }

  def run(pathParams: PathParams, request: Request[F]): F[Response[F]] = {
    val run: F[O] = for {
      metadata <- getMetadata(pathParams, request)
      input <- extractInput.run((metadata, request))
      output <- (impl(endpoint.wrap(input)): F[O])
    } yield output

    run.recoverWith(transformError).attempt.flatMap {
      case Left(error)   => errorResponse(error)
      case Right(output) => successResponse(output)
    }
  }

  private val inputSchema: Schema[I] = endpoint.input
  private val outputSchema: Schema[O] = endpoint.output

  private val inputMetadataDecoder =
    Metadata.PartialDecoder.fromSchema(inputSchema)
  private implicit val outputEntityEncoder: EntityEncoder[F, O] =
    codecs.compileEntityEncoder(outputSchema)
  private val outputMetadataEncoder =
    Metadata.Encoder.fromSchema(outputSchema)
  private implicit val httpContractErrorCodec
      : EntityEncoder[F, HttpContractError] =
    codecs.compileEntityEncoder(HttpContractError.schema)

  private val transformError: PartialFunction[Throwable, F[O]] = {
    case e @ endpoint.Error(_, _) => F.raiseError(e)
    case scala.util.control.NonFatal(other)
        if errorTransformation.isDefinedAt(other) =>
      errorTransformation(other).flatMap(F.raiseError)
  }



  // format: off
  private val extractInput: (Metadata, Request[F]) ==> I = {
    inputMetadataDecoder.total match {
      case Some(totalDecoder) =>
        Kleisli(totalDecoder.decode(_: Metadata).liftTo[F]).local(_._1)
      case None =>
        // NB : only compiling the input codec if the data cannot be
        // totally extracted from the metadata.
        implicit val inputCodec = codecs.compilePartialEntityDecoder(inputSchema)
        Kleisli { case (metadata, request) =>
          for {
            metadataPartial <- inputMetadataDecoder.decode(metadata).liftTo[F]
            bodyPartial <- request.as[BodyPartial[I]]
          } yield metadataPartial.combine(bodyPartial)
        }
    }
  }
  // format: on

  private def putHeaders(m: Message[F], headers: Headers) =
    m.putHeaders(headers.headers)

  private def status(code: Int): Status = Status
    .fromInt(code)
    .getOrElse(sys.error(s"Invalid status code: $code"))

  private def getMetadata(pathParams: PathParams, request: Request[F]) =
    Metadata(
      path = pathParams,
      headers = getHeaders(request),
      query = request.uri.query.pairs
        .collect {
          case (name, None)        => name -> "true"
          case (name, Some(value)) => name -> value
        }
        .groupBy(_._1)
        .map { case (k, v) => k -> v.map(_._2).toList }
    ).pure[F]

  private def successResponse(output: O): F[Response[F]] = {
    val outputMetadata = outputMetadataEncoder.encode(output)
    val outputHeaders = toHeaders(outputMetadata.headers)
    val successCode = status(httpEndpoint.code)
    putHeaders(Response[F](successCode), outputHeaders)
      .withEntity(output)
      .pure[F]
  }

  private def errorResponse(throwable: Throwable): F[Response[F]] = {

    def errorHeaders(errorLabel: String, metadata: Metadata): Headers =
      toHeaders(metadata.headers)
        .put(
          Header.Raw(CIString(errorTypeHeader), errorLabel)
        )

    def processAlternative[ErrorUnion, ErrorType](
        altAndValue: Alt.SchemaAndValue[ErrorUnion, ErrorType]
    ): Response[F] = {
      val errorSchema = altAndValue.alt.instance
      val errorValue = altAndValue.value
      val errorCode =
        http.HttpStatusCode.fromSchema(errorSchema).code(errorValue, 500)
      implicit val errorCodec = codecs.compileEntityEncoder(errorSchema)
      val metadataEncoder = Metadata.Encoder.fromSchema(errorSchema)
      val metadata = metadataEncoder.encode(errorValue)
      val headers = errorHeaders(altAndValue.alt.label, metadata)
      val status =
        Status.fromInt(errorCode).getOrElse(Status.InternalServerError)
      Response(status, headers = headers).withEntity(errorValue)
    }

    throwable
      .pure[F]
      .flatMap {
        case e: HttpContractError =>
          Response[F](Status.BadRequest).withEntity(e).pure[F]
        case endpoint.Error((errorable, e)) =>
          processAlternative(errorable.error.dispatch(e)).pure[F]
        case e: Throwable =>
          F.raiseError(e)
      }
  }

}
