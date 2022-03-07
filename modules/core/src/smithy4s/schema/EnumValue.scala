package smithy4s
package schema

case class EnumValue[E](name: String, value: E, hints: Hints)
