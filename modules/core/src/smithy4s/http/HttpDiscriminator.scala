package smithy4s.http

import smithy4s.ShapeId

sealed trait HttpDiscriminator extends Product with Serializable

object HttpErrorDiscriminator {

  // format: off
  final case class FullId(shapeId: ShapeId) extends HttpDiscriminator
  final case class NameOnly(name: String) extends HttpDiscriminator
  final case class StatusCode(int: Int) extends HttpDiscriminator
  // format: on

  def fromMetadata(
      errorTypeHeader: CaseInsensitive,
      metadata: Metadata
  ): Option[HttpDiscriminator] = {
    metadata.statusCode.map(code =>
      fromStatusOrHeader(errorTypeHeader, code, metadata.headers)
    )
  }

  def fromStatusOrHeader(
      discriminatingHeaderName: CaseInsensitive,
      statusCode: Int,
      headers: Map[CaseInsensitive, Seq[String]]
  ): HttpDiscriminator = {
    headers
      .get(discriminatingHeaderName)
      .flatMap(_.headOption)
      .map(errorType =>
        ShapeId
          .parse(errorType)
          .map(HttpErrorDiscriminator.FullId(_))
          .getOrElse(HttpErrorDiscriminator.NameOnly(errorType))
      )
      .getOrElse(
        HttpErrorDiscriminator.StatusCode(statusCode)
      )
  }

}
