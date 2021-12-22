package smithy4s.example

import smithy4s.syntax._

case class ServerError(message: Option[String] = None) extends Throwable {
  override def getMessage() : String = message.orNull
}
object ServerError {
  def namespace: String = NAMESPACE
  val name: String = "ServerError"

  val hints : smithy4s.Hints = smithy4s.Hints(
    smithy.api.Error.SERVER,
  )

  val schema: smithy4s.Schema[ServerError] = struct(
    string.optional[ServerError]("message", _.message),
  ){
    ServerError.apply
  }.withHints(hints)
  implicit val staticSchema : schematic.Static[smithy4s.Schema[ServerError]] = schematic.Static(schema)
}