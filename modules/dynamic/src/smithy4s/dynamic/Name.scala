package smithy4s.dynamic

import smithy4s.Hints

case class Name(name: String)

object Name extends Hints.Key.Companion[Name] {
  val namespace: String = "smithy4s.dynamic"
  val name = "Name"
}
