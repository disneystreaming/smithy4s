package smithy4s
package schema

case class EnumValue[E](
    stringValue: String,
    ordinal: Int,
    value: E,
    hints: Hints
)
