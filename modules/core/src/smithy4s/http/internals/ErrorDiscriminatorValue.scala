package smithy4s
package http.internals

import smithy4s.schema.Schema._

private[smithy4s] case class ErrorDiscriminatorValue(name: String)

private[smithy4s] object ErrorDiscriminatorValue
    extends ShapeTag.Companion[ErrorDiscriminatorValue] {

  val id: ShapeId =
    ShapeId("smithy4s.http.internals", "ErrorDiscriminatorValue")

  val schema: Schema[ErrorDiscriminatorValue] =
    string
      .biject[ErrorDiscriminatorValue](ErrorDiscriminatorValue(_))(_.name)

}
