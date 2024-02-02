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

package smithy4s.http

import smithy4s.ShapeId

sealed trait HttpDiscriminator extends Product with Serializable

object HttpDiscriminator {

  final case class FullId private (shapeId: ShapeId) extends HttpDiscriminator
  object FullId {
    def apply(shapeId: ShapeId): FullId = {
      new FullId(shapeId)
    }
  }

  final case class NameOnly private (name: String) extends HttpDiscriminator
  object NameOnly {
    def apply(name: String): NameOnly = {
      new NameOnly(name)
    }
  }

  final case class StatusCode private (int: Int) extends HttpDiscriminator
  object StatusCode {
    def apply(int: Int): StatusCode = {
      new StatusCode(int)
    }
  }

  case object Undetermined extends HttpDiscriminator

  def fromResponse(
      discriminatingHeaderNames: List[String],
      response: HttpResponse[Any]
  ): HttpDiscriminator =
    fromStatusOrHeader(
      discriminatingHeaderNames,
      response.statusCode,
      response.headers
    )

  def fromMetadata(
      discriminatingHeaderNames: List[String],
      metadata: Metadata
  ): Option[HttpDiscriminator] = {
    metadata.statusCode.map(code =>
      fromStatusOrHeader(discriminatingHeaderNames, code, metadata.headers)
    )
  }

  def fromStatusOrHeader(
      discriminatingHeaderNames: List[String],
      statusCode: Int,
      headers: Map[CaseInsensitive, Seq[String]]
  ): HttpDiscriminator = {
    discriminatingHeaderNames.iterator
      .map(CaseInsensitive(_))
      .map(headers.get)
      .collectFirst { case Some(h) => h }
      .flatMap(_.headOption)
      .map(errorType =>
        ShapeId
          .parse(errorType)
          .map(FullId(_))
          .getOrElse(NameOnly(errorType))
      )
      .getOrElse(
        StatusCode(statusCode)
      )
  }

}
