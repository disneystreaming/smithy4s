package smithy4s.http

import smithy4s.ShapeId

sealed trait HttpDiscriminator extends Product with Serializable

object HttpDiscriminator {

  // format: off
  final case class FullId(shapeId: ShapeId) extends HttpDiscriminator
  final case class NameOnly(name: String) extends HttpDiscriminator
  final case class StatusCode(int: Int) extends HttpDiscriminator
  // format: on

  def fromMetadata(
      discriminatingHeaderName: String,
      metadata: Metadata
  ): Option[HttpDiscriminator] = {
    metadata.statusCode.map(code =>
      fromStatusOrHeader(discriminatingHeaderName, code, metadata.headers)
    )
  }

  def fromStatusOrHeader(
      discriminatingHeaderName: String,
      statusCode: Int,
      headers: Map[CaseInsensitive, Seq[String]]
  ): HttpDiscriminator = {
    headers
      .get(CaseInsensitive(discriminatingHeaderName))
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
