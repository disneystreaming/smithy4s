package smithy4s
package schema

case class EnumValue[E](name: String, ordinal: Int, value: E, hints: Hints)
