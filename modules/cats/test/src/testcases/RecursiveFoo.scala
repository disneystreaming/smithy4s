package smithy4s.interopcats.testcases


import smithy4s.schema.Schema
import smithy4s.schema.Schema._
import smithy4s.ShapeId

case class RecursiveFoo(foo: Option[RecursiveFoo])

object RecursiveFoo {
  val schema: Schema[RecursiveFoo] =
    recursive {
      val foos = schema.optional[RecursiveFoo]("foo", _.foo)
      struct(foos)(RecursiveFoo.apply)
    }.withId(ShapeId("", "Foo"))
}