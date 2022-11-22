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
package http4s
package internals

import cats.data.Kleisli
import cats.syntax.all._
import org.http4s.EntityEncoder
import org.http4s.Headers
import org.http4s.Message
import org.http4s.Method
import org.http4s.Request
import org.http4s.Response
import org.http4s.Status
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
      compilerContext: CompilerContext[F],
      errorTransformation: PartialFunction[Throwable, F[Throwable]]
  ): Option[SmithyHttp4sServerEndpoint[F]] =
    HttpEndpoint.cast(endpoint).map { httpEndpoint =>
      new SmithyHttp4sServerEndpointImpl[F, Op, I, E, O, SI, SO](
        impl,
        endpoint,
        httpEndpoint,
        compilerContext,
        errorTransformation
      )
    }

}

// format: off
private[smithy4s] class SmithyHttp4sServerEndpointImpl[F[_], Op[_, _, _, _, _], I, E, O, SI, SO](
    impl: Interpreter[Op, F],
    endpoint: Endpoint[Op, I, E, O, SI, SO],
    httpEndpoint: HttpEndpoint[I],
    compilerContext: CompilerContext[F],
    errorTransformation: PartialFunction[Throwable, F[Throwable]],
)(implicit F: EffectCompat[F]) extends SmithyHttp4sServerEndpoint[F] {
// format: on
  import compilerContext._

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
    Metadata.PartialDecoder.fromSchema(inputSchema, metadataDecoderCache)
  private implicit val outputEntityEncoder: EntityEncoder[F, O] =
    entityCompiler.compileEntityEncoder(outputSchema, entityCache)

  private val outputMetadataCache = Metadata.Encoder.createCache()
  private val outputMetadataEncoder =
    Metadata.Encoder.fromSchema(outputSchema, outputMetadataCache)
  private implicit val httpContractErrorCodec
      : EntityEncoder[F, HttpContractError] =
    entityCompiler.compileEntityEncoder(HttpContractError.schema, entityCache)

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
        implicit val inputCodec = entityCompiler.compilePartialEntityDecoder(inputSchema, entityCache)
        Kleisli { case (metadata, request) =>
          for {
            metadataPartial <- inputMetadataDecoder.decode(metadata).liftTo[F]
            bodyPartial <- request.as[BodyPartial[I]]
            decoded <- metadataPartial.combineCatch(bodyPartial).liftTo[F]
          } yield decoded
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
    val statusCode = outputMetadata.statusCode.getOrElse(httpEndpoint.code)
    val httpStatus = status(statusCode)

    putHeaders(Response[F](httpStatus), outputHeaders)
      .withEntity(output)
      .pure[F]
  }

  def compileErrorable(errorable: Errorable[E]): E => Response[F] = {
    def errorHeaders(errorLabel: String, metadata: Metadata): Headers =
      toHeaders(metadata.headers).put(errorTypeHeader -> errorLabel)
    val errorUnionSchema = errorable.error
    val dispatcher =
      Alt.Dispatcher(errorUnionSchema.alternatives, errorUnionSchema.dispatch)
    type ErrorEncoder[Err] = Err => Response[F]
    val precompiler = new Alt.Precompiler[Schema, ErrorEncoder] {
      def apply[Err](
          label: String,
          errorSchema: Schema[Err]
      ): ErrorEncoder[Err] = {
        implicit val errorCodec =
          entityCompiler.compileEntityEncoder(errorSchema, entityCache)
        val metadataEncoder = Metadata.Encoder.fromSchema(errorSchema)
        errorValue => {
          val errorCode =
            http.HttpStatusCode.fromSchema(errorSchema).code(errorValue, 500)
          val metadata = metadataEncoder.encode(errorValue)
          val headers = errorHeaders(label, metadata)
          val status =
            Status.fromInt(errorCode).getOrElse(Status.InternalServerError)
          Response(status, headers = headers).withEntity(errorValue)
        }
      }
    }
    dispatcher.compile(precompiler)
  }

  val errorResponse: Throwable => F[Response[F]] = {
    endpoint.errorable match {
      case Some(errorable) =>
        val processError: E => Response[F] = compileErrorable(errorable)

        {
          case e: HttpContractError =>
            Response[F](Status.BadRequest).withEntity(e).pure[F]
          case endpoint.Error((_, e)) =>
            F.pure(processError(e))
          case e: Throwable =>
            F.raiseError(e)
        }

      case None => {
        case e: HttpContractError =>
          Response[F](Status.BadRequest).withEntity(e).pure[F]
        case e: Throwable =>
          F.raiseError(e)
      }
    }
  }

}
