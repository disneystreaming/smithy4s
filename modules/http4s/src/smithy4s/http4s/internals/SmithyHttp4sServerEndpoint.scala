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
import smithy4s.kinds._
import org.http4s.HttpApp
import org.typelevel.vault.Key

/**
  * A construct that encapsulates a smithy4s endpoint, and exposes
  * http4s specific semantics.
  */
private[http4s] trait SmithyHttp4sServerEndpoint[F[_]] {
  def method: org.http4s.Method
  def matches(path: Array[String]): Option[PathParams]
  def httpApp: HttpApp[F]

  def matchTap(
      path: Array[String]
  ): Option[(SmithyHttp4sServerEndpoint[F], PathParams)] =
    matches(path).map(this -> _)
}

private[http4s] object SmithyHttp4sServerEndpoint {

  // format: off
  def make[F[_]: EffectCompat, Op[_, _, _, _, _], I, E, O, SI, SO](
      impl: FunctorInterpreter[Op, F],
      endpoint: Endpoint[Op, I, E, O, SI, SO],
      compilerContext: CompilerContext[F],
      errorTransformation: PartialFunction[Throwable, F[Throwable]],
      middleware: ServerEndpointMiddleware.EndpointMiddleware[F, Op],
      pathParamsKey: Key[PathParams]
  ): Either[
    HttpEndpoint.HttpEndpointError,
    SmithyHttp4sServerEndpoint[F]
  ] =
  // format: on
    HttpEndpoint.cast(endpoint).flatMap { httpEndpoint =>
      toHttp4sMethod(httpEndpoint.method)
        .leftMap { e =>
          HttpEndpoint.HttpEndpointError(
            "Couldn't parse HTTP method: " + e
          )
        }
        .map { method =>
          new SmithyHttp4sServerEndpointImpl[F, Op, I, E, O, SI, SO](
            impl,
            endpoint,
            method,
            httpEndpoint,
            compilerContext,
            errorTransformation,
            middleware,
            pathParamsKey
          )
        }
    }

}

// format: off
private[http4s] class SmithyHttp4sServerEndpointImpl[F[_], Op[_, _, _, _, _], I, E, O, SI, SO](
    impl: FunctorInterpreter[Op, F],
    endpoint: Endpoint[Op, I, E, O, SI, SO],
    val method: Method,
    httpEndpoint: HttpEndpoint[I],
    compilerContext: CompilerContext[F],
    errorTransformation: PartialFunction[Throwable, F[Throwable]],
    middleware: ServerEndpointMiddleware.EndpointMiddleware[F, Op],
    pathParamsKey: Key[PathParams]
)(implicit F: EffectCompat[F]) extends SmithyHttp4sServerEndpoint[F] {
// format: on
  import compilerContext._

  type ==>[A, B] = Kleisli[F, A, B]

  def matches(path: Array[String]): Option[PathParams] = {
    httpEndpoint.matches(path)
  }

  private val applyMiddleware: HttpApp[F] => HttpApp[F] = { app =>
    middleware(endpoint)(app).handleErrorWith(error =>
      Kleisli.liftF(errorResponse(error))
    )
  }

  override val httpApp: HttpApp[F] =
    httpAppErrorHandle(applyMiddleware(HttpApp[F] { req =>
      val pathParams = req.attributes.lookup(pathParamsKey).getOrElse(Map.empty)

      val run: F[O] = for {
        metadata <- getMetadata(pathParams, req)
        input <- extractInput(metadata, req)
        output <- (impl(endpoint.wrap(input)): F[O])
      } yield output

      run
        .recoverWith(transformError)
        .map(successResponse)
    }))

  private def httpAppErrorHandle(app: HttpApp[F]): HttpApp[F] = {
    app
      .recoverWith {
        case error if errorTransformation.isDefinedAt(error) =>
          Kleisli.liftF(errorTransformation.apply(error).flatMap(errorResponse))
      }
      .handleErrorWith { error => Kleisli.liftF(errorResponse(error)) }
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

  private val extractInput: (Metadata, Request[F]) => F[I] = {
    inputMetadataDecoder.total match {
      case Some(totalDecoder) =>
        (metadata, request) =>
          request.body.compile.drain *>
            totalDecoder.decode(metadata).liftTo[F]
      case None =>
        // NB: only compiling the input codec if the data cannot be
        // totally extracted from the metadata.
        implicit val inputCodec =
          entityCompiler.compilePartialEntityDecoder(inputSchema, entityCache)
        (metadata, request) =>
          for {
            metadataPartial <- inputMetadataDecoder.decode(metadata).liftTo[F]
            bodyPartial <- request.as[BodyPartial[I]]
            decoded <- metadataPartial.combineCatch(bodyPartial).liftTo[F]
          } yield decoded
    }
  }

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

  private def successResponse(output: O): Response[F] = {
    val outputMetadata = outputMetadataEncoder.encode(output)
    val outputHeaders = toHeaders(outputMetadata.headers)
    val statusCode = outputMetadata.statusCode.getOrElse(httpEndpoint.code)
    val httpStatus = status(statusCode)

    putHeaders(Response[F](httpStatus), outputHeaders)
      .withEntity(output)
  }

  private def compileErrorable(errorable: Errorable[E]): E => Response[F] = {
    def errorHeaders(errorLabel: String, metadata: Metadata): Headers =
      toHeaders(metadata.headers).put(errorTypeHeader -> errorLabel)
    val errorUnionSchema = errorable.error
    val dispatcher =
      Alt.Dispatcher.fromUnion(errorUnionSchema)
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
