package smithy4s

sealed trait SchemaType

object SchemaType {
  case object Primitive extends SchemaType
  case object Collection extends SchemaType
  case object Map extends SchemaType
  case object Enumeration extends SchemaType
  case object Struct extends SchemaType
  case object Union extends SchemaType
  case object Bijection extends SchemaType
  case object Refinement extends SchemaType
  case object Lazily extends SchemaType
}
