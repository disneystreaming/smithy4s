package smithy4s.example

package object imp {
  type ImportService[F[_]] = smithy4s.kinds.FunctorAlgebra[ImportServiceGen, F]
  object ImportService extends smithy4s.Service.Provider[ImportServiceGen, ImportServiceOperation] {
    def apply[F[_]](implicit F: ImportService[F]): F.type = F
    def service: smithy4s.Service[ImportServiceGen, ImportServiceOperation] = ImportServiceGen
    val id: smithy4s.ShapeId = service.id
  }


}