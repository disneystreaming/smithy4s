/*
 *  Copyright 2021-2024 Disney Streaming
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

import smithy.api._
import smithy4s.http.HttpBinding.HeaderBinding
import smithy4s.http.HttpBinding.HeaderPrefixBinding
import smithy4s.http.HttpBinding.PathBinding
import smithy4s.http.HttpBinding.QueryBinding
import smithy4s.http.HttpBinding.QueryParamsBinding
import smithy4s.http.HttpBinding.StatusCodeBinding
import smithy4s.internals.InputOutput
import smithy4s.schema.Schema._

sealed abstract class HttpBinding(val tpe: HttpBinding.Type)
    extends Product
    with Serializable {

  def show: String = this match {
    case hb: HeaderBinding        => s"Header ${hb.httpName}"
    case hpb: HeaderPrefixBinding => s"Headers prefixed by ${hpb.prefix}"
    case qb: QueryBinding         => s"Query parameter ${qb.httpName}"
    case QueryParamsBinding       => "Query parameters"
    case pb: PathBinding          => s"Path parameter ${pb.httpName}"
    case StatusCodeBinding        => "Status code"
  }

}

object HttpBinding extends ShapeTag.Companion[HttpBinding] {

  val id: ShapeId = ShapeId("smithy4s.http", "HttpBinding")

  sealed trait Type
  object Type {
    case object HeaderType extends Type
    case object QueryType extends Type
    case object PathType extends Type
    case object StatusCodeType extends Type
  }

  case class HeaderBinding private (httpName: CaseInsensitive)
      extends HttpBinding(Type.HeaderType) {
    def withHttpName(value: CaseInsensitive): HeaderBinding = {
      copy(httpName = value)
    }

  }
  case class HeaderPrefixBinding private (prefix: String)
      extends HttpBinding(Type.HeaderType) {
    def withPrefix(value: String): HeaderPrefixBinding = {
      copy(prefix = value)
    }

  }
  case class QueryBinding private (httpName: String)
      extends HttpBinding(Type.QueryType) {
    def withHttpName(value: String): QueryBinding = {
      copy(httpName = value)
    }

  }
  case object QueryParamsBinding extends HttpBinding(Type.QueryType) {
    val schema: Schema[QueryParamsBinding.type] = constant(QueryParamsBinding)
  }
  case class PathBinding private (httpName: String)
      extends HttpBinding(Type.PathType) {
    def withHttpName(value: String): PathBinding = {
      copy(httpName = value)
    }

  }
  case object StatusCodeBinding extends HttpBinding(Type.StatusCodeType) {
    val schema: Schema[StatusCodeBinding.type] = constant(StatusCodeBinding)
  }

  object HeaderBinding {
    @scala.annotation.nowarn(
      "msg=private method unapply in object HeaderBinding is never used"
    )
    private def unapply(c: HeaderBinding): Option[HeaderBinding] = Some(c)
    def apply(httpName: CaseInsensitive): HeaderBinding = {
      new HeaderBinding(httpName)
    }

    val schema: Schema[HeaderBinding] =
      struct(string.required[HeaderBinding]("httpName", _.httpName.toString))(
        string => HeaderBinding(CaseInsensitive(string))
      )
  }
  object HeaderPrefixBinding {
    @scala.annotation.nowarn(
      "msg=private method unapply in object HeaderPrefixBinding is never used"
    )
    private def unapply(c: HeaderPrefixBinding): Option[HeaderPrefixBinding] =
      Some(c)
    def apply(prefix: String): HeaderPrefixBinding = {
      new HeaderPrefixBinding(prefix)
    }

    val schema: Schema[HeaderPrefixBinding] =
      struct(string.required[HeaderPrefixBinding]("prefix", _.prefix))(
        HeaderPrefixBinding.apply
      )
  }
  object QueryBinding {
    @scala.annotation.nowarn(
      "msg=private method unapply in object QueryBinding is never used"
    )
    private def unapply(c: QueryBinding): Option[QueryBinding] = Some(c)
    def apply(httpName: String): QueryBinding = {
      new QueryBinding(httpName)
    }

    val schema: Schema[QueryBinding] =
      struct(string.required[QueryBinding]("httpName", _.httpName))(
        QueryBinding.apply
      )
  }
  object PathBinding {
    @scala.annotation.nowarn(
      "msg=private method unapply in object PathBinding is never used"
    )
    private def unapply(c: PathBinding): Option[PathBinding] = Some(c)
    def apply(httpName: String): PathBinding = {
      new PathBinding(httpName)
    }

    val schema: Schema[PathBinding] =
      struct(string.required[PathBinding]("httpName", _.httpName))(
        PathBinding.apply
      )
  }
  implicit val schema: Schema[HttpBinding] = {
    val header = HeaderBinding.schema.oneOf[HttpBinding]("header")
    val query = QueryBinding.schema.oneOf[HttpBinding]("query")
    val path = PathBinding.schema.oneOf[HttpBinding]("path")
    val queryParams =
      QueryParamsBinding.schema.oneOf[HttpBinding]("queryParams")
    val headerPrefix =
      HeaderPrefixBinding.schema.oneOf[HttpBinding]("headerPrefix")
    val status =
      StatusCodeBinding.schema.oneOf[HttpBinding]("statusCode")

    union(header, query, path, queryParams, headerPrefix, status) {
      case _: HeaderBinding       => 0
      case _: QueryBinding        => 1
      case _: PathBinding         => 2
      case QueryParamsBinding     => 3
      case _: HeaderPrefixBinding => 4
      case StatusCodeBinding      => 5
    }
  }

  private[smithy4s] def fromHints(
      field: String,
      fieldHints: Hints,
      shapeHints: Hints
  ): Option[HttpBinding] = shapeHints match {
    case InputOutput.hint(InputOutput.Input) =>
      fromHintsInput(field, fieldHints)
    case InputOutput.hint(InputOutput.Output) =>
      fromHintsOutput(field, fieldHints)
    case smithy.api.Error.hint(_) => fromHintsOutput(field, fieldHints)
    case _                        => None
  }

  private def fromHintsInput(
      field: String,
      fieldHints: Hints
  ): Option[HttpBinding] = {
    fieldHints.get(HttpLabel).map { case HttpLabel() =>
      PathBinding(field)
    } orElse fieldHints.get(HttpQuery).map { case HttpQuery(name) =>
      QueryBinding(name)
    } orElse fieldHints.get(HttpHeader).map { case HttpHeader(name) =>
      HeaderBinding(CaseInsensitive(name))
    } orElse fieldHints.get(HttpPrefixHeaders).map {
      case HttpPrefixHeaders(prefix) =>
        HeaderPrefixBinding(prefix)
    } orElse fieldHints.get(HttpQueryParams).map { _ =>
      QueryParamsBinding
    }
  }

  private def fromHintsOutput(
      field: String,
      fieldHints: Hints
  ): Option[HttpBinding] = {
    fieldHints.get(HttpHeader).map { case HttpHeader(name) =>
      HeaderBinding(CaseInsensitive(name))
    } orElse fieldHints.get(HttpPrefixHeaders).map {
      case HttpPrefixHeaders(prefix) =>
        HeaderPrefixBinding(prefix)
    } orElse fieldHints.get[HttpResponseCode].map { _ => StatusCodeBinding }
  }

}
