package smithy4s.interopcats.testcases

import smithy4s.schema.Schema._
import smithy4s.schema.Schema
import smithy4s.ShapeId

sealed trait IntOrInt 
object IntOrInt {
  case class IntValue0(value: Int) extends IntOrInt
  case class IntValue1(value: Int) extends IntOrInt

  val schema: Schema[IntOrInt] = {
    val intValue0 = int.oneOf[IntOrInt]("intValue0", IntValue0(_))
    val intValue1 = int.oneOf[IntOrInt]("intValue1", IntValue1(_))
    union(intValue0, intValue1){
      case IntValue0(value) => intValue0(value)
      case IntValue1(value) => intValue1(value)}
  }.withId(ShapeId("","IntOrInt"))

}
