package smithy4s.schema

sealed trait EnumTag

object EnumTag {
    case object StringEnum extends EnumTag
    case object IntEnum extends EnumTag
}
