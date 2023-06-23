package smithy4s.example

package object product {
  type ObjectService[F[_]] = smithy4s.kinds.FunctorAlgebra[ObjectServiceGen, F]
  val ObjectService = ObjectServiceGen


}