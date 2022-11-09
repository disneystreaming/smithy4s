package smithy4s

package object example {
  type StreamedObjects[F[_]] = smithy4s.kinds.FunctorAlgebra[StreamedObjectsGen, F]
  object StreamedObjects extends smithy4s.Service.Provider[StreamedObjectsGen] {
    def apply[F[_]](implicit F: StreamedObjects[F]): F.type = F
    def service: smithy4s.Service[StreamedObjectsGen] = StreamedObjectsGen
    val id: smithy4s.ShapeId = service.id
  }
  type FooService[F[_]] = smithy4s.kinds.FunctorAlgebra[FooServiceGen, F]
  object FooService extends smithy4s.Service.Provider[FooServiceGen] {
    def apply[F[_]](implicit F: FooService[F]): F.type = F
    def service: smithy4s.Service[FooServiceGen] = FooServiceGen
    val id: smithy4s.ShapeId = service.id
  }
  type BrandService[F[_]] = smithy4s.kinds.FunctorAlgebra[BrandServiceGen, F]
  object BrandService extends smithy4s.Service.Provider[BrandServiceGen] {
    def apply[F[_]](implicit F: BrandService[F]): F.type = F
    def service: smithy4s.Service[BrandServiceGen] = BrandServiceGen
    val id: smithy4s.ShapeId = service.id
  }
  type ObjectService[F[_]] = smithy4s.kinds.FunctorAlgebra[ObjectServiceGen, F]
  object ObjectService extends smithy4s.Service.Provider[ObjectServiceGen] {
    def apply[F[_]](implicit F: ObjectService[F]): F.type = F
    def service: smithy4s.Service[ObjectServiceGen] = ObjectServiceGen
    val id: smithy4s.ShapeId = service.id
  }
  type NameCollision[F[_]] = smithy4s.kinds.FunctorAlgebra[NameCollisionGen, F]
  object NameCollision extends smithy4s.Service.Provider[NameCollisionGen] {
    def apply[F[_]](implicit F: NameCollision[F]): F.type = F
    def service: smithy4s.Service[NameCollisionGen] = NameCollisionGen
    val id: smithy4s.ShapeId = service.id
  }

  type StreamedBlob = smithy4s.example.StreamedBlob.Type
  type SomeValue = smithy4s.example.SomeValue.Type
  type TestString = smithy4s.example.TestString.Type
  type Age = smithy4s.example.Age.Type
  type BucketName = smithy4s.example.BucketName.Type
  type Name = smithy4s.example.Name.Type
  type ObjectKey = smithy4s.example.ObjectKey.Type
  type OrderNumber = smithy4s.example.OrderNumber.Type
  type UnwrappedFancyList = smithy4s.example.UnwrappedFancyList.Type
  type ArbitraryData = smithy4s.example.ArbitraryData.Type
  type DogName = smithy4s.example.DogName.Type
  type SomeVector = smithy4s.example.SomeVector.Type
  type FancyList = smithy4s.example.FancyList.Type
  type PersonAge = smithy4s.example.PersonAge.Type
  type ObjectSize = smithy4s.example.ObjectSize.Type
  type SomeIndexSeq = smithy4s.example.SomeIndexSeq.Type
  type StringList = smithy4s.example.StringList.Type

}