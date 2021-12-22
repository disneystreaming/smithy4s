package smithy4s.example

import smithy4s.syntax._

case class GetFooOutput(foo: Option[Foo] = None)
object GetFooOutput {
  def namespace: String = NAMESPACE
  val name: String = "GetFooOutput"

  val hints : smithy4s.Hints = smithy4s.Hints()

  val schema: smithy4s.Schema[GetFooOutput] = struct(
    Foo.schema.optional[GetFooOutput]("foo", _.foo),
  ){
    GetFooOutput.apply
  }
  implicit val staticSchema : schematic.Static[smithy4s.Schema[GetFooOutput]] = schematic.Static(schema)
}