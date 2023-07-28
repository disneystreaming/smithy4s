package smithy4s

import smithy4s.schema.Schema
import smithy4s.schema.EnumValue

object OpenStringEnumTest {

  sealed trait OpenStringEnum
  object OpenStringEnum {
    case object Known extends OpenStringEnum
    case class Unknown(str: String) extends OpenStringEnum

    def toEnumValue(o: OpenStringEnum): EnumValue[OpenStringEnum] = o match {
      case Known        => EnumValue("known", 1, Known, "Known", Hints.empty)
      case unknown @ Unknown(str) => EnumValue(str, -1, unknown, "Unknown", Hints.empty)
    }

    val knownValues: List[OpenStringEnum] = List(
      Known
    )

    val unknown : String => OpenStringEnum

    val schema: Schema[OpenStringEnum] =
      Schema.enumeration(
        toEnumValue,
        EnumTag.OpenStringEnum(str => Unknown(str)),
        knownValues.map(toEnumValue)
      )

  }

}
