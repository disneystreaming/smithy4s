package smithy4s.example

import smithy4s.syntax._

case class NoMoreSpace(message: String, foo: Option[Foo] = None) extends Throwable {
  override def getMessage() : String = message
}
object NoMoreSpace {
  def namespace: String = NAMESPACE
  val name: String = "NoMoreSpace"

  val hints : smithy4s.Hints = smithy4s.Hints(
    smithy.api.Error.SERVER,
    smithy.api.HttpError(507),
  )

  val schema: smithy4s.Schema[NoMoreSpace] = struct(
    string.required[NoMoreSpace]("message", _.message).withHints(smithy.api.Required()),
    Foo.schema.optional[NoMoreSpace]("foo", _.foo),
  ){
    NoMoreSpace.apply
  }.withHints(hints)
  implicit val staticSchema : schematic.Static[smithy4s.Schema[NoMoreSpace]] = schematic.Static(schema)
}