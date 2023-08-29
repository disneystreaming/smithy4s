package smithy4s
package http.internals

import smithy4s.schema.Schema._

private[smithy4s] case class StaticUrlFormElements(
    elements: List[(String, String)]
)

private[smithy4s] object StaticUrlFormElements
    extends ShapeTag.Companion[StaticUrlFormElements] {

  val id: ShapeId = ShapeId("smithy4s.http.internals", "StaticUrlFormElements")

  val schema: Schema[StaticUrlFormElements] =
    list(tuple(string, string))
      .biject[StaticUrlFormElements](StaticUrlFormElements(_))(_.elements)

}
