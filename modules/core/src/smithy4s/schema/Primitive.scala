package smithy4s
package schema

sealed trait Primitive {
  type T
}

object Primitive {
  type Aux[A] = Primitive { type T = A }

  case object PInt extends Primitive { type T = Int }
  case object PString extends Primitive { type T = String }
}
