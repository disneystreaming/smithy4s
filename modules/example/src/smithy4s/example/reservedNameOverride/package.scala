package smithy4s.example

package object reservedNameOverride {
  type ReservedNameOverrideService[F[_]] = smithy4s.Monadic[ReservedNameOverrideServiceGen, F]
  object ReservedNameOverrideService extends smithy4s.Service.Provider[ReservedNameOverrideServiceGen, ReservedNameOverrideServiceOperation] {
    def apply[F[_]](implicit F: ReservedNameOverrideService[F]): F.type = F
    def service: smithy4s.Service[ReservedNameOverrideServiceGen, ReservedNameOverrideServiceOperation] = ReservedNameOverrideServiceGen
    val id: smithy4s.ShapeId = service.id
  }


}