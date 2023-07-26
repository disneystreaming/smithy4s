package smithy4s.example


@deprecated(message = "A compelling reason", since = "0.0.1")
trait DeprecatedMixin {
  @deprecated(message = "N/A", since = "N/A") def strings: Option[List[String]]
}