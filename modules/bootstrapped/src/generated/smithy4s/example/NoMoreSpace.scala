package smithy4s.example

import smithy.api.Error
import smithy.api.HttpError
import smithy.api.Required
import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.schema.Schema.string
import smithy4s.schema.Schema.struct

/** @param foo
  *   Helpful information for Foo
  *   int, bigInt and bDec are useful number constructs
  *   The string case is there because.
  */
final case class NoMoreSpace(message: String, foo: Option[Foo] = None) extends Throwable {
  override def getMessage(): String = message
}
object NoMoreSpace extends ShapeTag.Companion[NoMoreSpace] {

  val message = string.required[NoMoreSpace]("message", _.message, n => c => c.copy(message = n)).addHints(Required())
  val foo = Foo.schema.optional[NoMoreSpace]("foo", _.foo, n => c => c.copy(foo = n))

  implicit val schema: Schema[NoMoreSpace] = struct(
    message,
    foo,
  ){
    NoMoreSpace.apply
  }
  .withId(ShapeId("smithy4s.example", "NoMoreSpace"))
  .addHints(
    Hints(
      Error.SERVER.widen,
      HttpError(507),
    )
  )
}
