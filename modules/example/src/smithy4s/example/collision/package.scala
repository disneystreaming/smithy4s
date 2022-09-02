package smithy4s.example

package object collision {
  type ReservedNameService[F[_]] = smithy4s.Monadic[ReservedNameServiceGen, F]
  object ReservedNameService extends smithy4s.Service.Provider[ReservedNameServiceGen, ReservedNameServiceOperation] {
    def apply[F[_]](implicit F: ReservedNameService[F]): F.type = F
    def service: smithy4s.Service[ReservedNameServiceGen, ReservedNameServiceOperation] = ReservedNameServiceGen
    val id: smithy4s.ShapeId = service.id
  }

  type MySet = smithy4s.example.collision.MySet.Type
  type MyMap = smithy4s.example.collision.MyMap.Type
  type MyList = smithy4s.example.collision.MyList.Type

}