package smithy4s.example

package object reservedNameOverride {
  type ReservedNameOverrideService[F[_]] = smithy4s.kinds.FunctorAlgebra[ReservedNameOverrideServiceGen, F]
  val ReservedNameOverrideService = ReservedNameOverrideServiceGen


}