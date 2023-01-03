package smithy4s.aws.query

final case class FormData(
    values: Map[List[String], String],
    primitive: Option[String]
) {
  lazy val render: Option[String] =
    primitive.orElse(
      if (values.isEmpty) None
      else
        Some(
          values
            .map { case (segments, value) =>
              segments.reverse.mkString(".") + "=" + value
            }
            .mkString("&")
        )
    )

  def addSegment(segment: String): FormData = primitive match {
    case Some(value) => FormData(Map(List(segment) -> value), None)
    case None =>
      FormData(
        values.map { case (segments, value) =>
          (segment :: segments, value)
        },
        None
      )
  }

  def childOf(parent: String): FormData =
    primitive match {
      case Some(value) => FormData(Map(List(parent) -> value), None)
      case None =>
        FormData(
          values.map { case (segments, value) =>
            (segments ::: List(parent), value)
          },
          None
        )
    }

  def combine(other: FormData): FormData =
    FormData(values ++ other.values, other.primitive)
}

object FormData {

  val empty: FormData = FormData(Map.empty, None)

  def simple(value: String): FormData = FormData(Map.empty, Some(value))

  def one(key: List[String], value: String): FormData =
    FormData(Map(key -> value), None)
}
