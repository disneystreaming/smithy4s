package smithy4s.interopcats.testcases

import smithy4s.schema.Schema._
import smithy4s.schema.Schema
import smithy4s.ShapeId

sealed trait IntOrString

object IntOrString {
  case class IntValue(value: Int) extends IntOrString

  case class StringValue(value: String) extends IntOrString

  val schema: Schema[IntOrString] = {
    val intValue = int.oneOf[IntOrString]("intValue", IntValue(_))
    val stringValue = string.oneOf[IntOrString]("stringValue", StringValue(_))
    union(intValue, stringValue) {
      case IntValue(int) => intValue(int)
      case StringValue(string) => stringValue(string)
    }.withId(ShapeId("", "IntOrString"))
  }
}
