package smithy4s

sealed trait SchemaType

/**
  * Tag for types used in Smithy4s schemas.
  * Used to allow default schema visitor implementations to know what type of
  * schema they are visiting.
  */
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
  case object Nullable extends SchemaType
}
