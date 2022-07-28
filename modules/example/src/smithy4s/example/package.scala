package smithy4s

package object example {
  type StreamedObjects[F[_]] = smithy4s.Monadic[StreamedObjectsGen, F]
  object StreamedObjects extends smithy4s.Service.Provider[StreamedObjectsGen, StreamedObjectsOperation] {
    def apply[F[_]](implicit F: StreamedObjects[F]): F.type = F
    def service : smithy4s.Service[StreamedObjectsGen, StreamedObjectsOperation] = StreamedObjectsGen
    val id: smithy4s.ShapeId = service.id
  }
  type FooService[F[_]] = smithy4s.Monadic[FooServiceGen, F]
  object FooService extends smithy4s.Service.Provider[FooServiceGen, FooServiceOperation] {
    def apply[F[_]](implicit F: FooService[F]): F.type = F
    def service : smithy4s.Service[FooServiceGen, FooServiceOperation] = FooServiceGen
    val id: smithy4s.ShapeId = service.id
  }
  type BrandService[F[_]] = smithy4s.Monadic[BrandServiceGen, F]
  object BrandService extends smithy4s.Service.Provider[BrandServiceGen, BrandServiceOperation] {
    def apply[F[_]](implicit F: BrandService[F]): F.type = F
    def service : smithy4s.Service[BrandServiceGen, BrandServiceOperation] = BrandServiceGen
    val id: smithy4s.ShapeId = service.id
  }
  type ObjectService[F[_]] = smithy4s.Monadic[ObjectServiceGen, F]
  object ObjectService extends smithy4s.Service.Provider[ObjectServiceGen, ObjectServiceOperation] {
    def apply[F[_]](implicit F: ObjectService[F]): F.type = F
    def service : smithy4s.Service[ObjectServiceGen, ObjectServiceOperation] = ObjectServiceGen
    val id: smithy4s.ShapeId = service.id
  }

  type ArbitraryData = smithy4s.example.ArbitraryData.Type
  type StreamedBlob = smithy4s.example.StreamedBlob.Type
  type SomeVector = smithy4s.example.SomeVector.Type
  type FancyList = smithy4s.example.FancyList.Type
  type SomeValue = smithy4s.example.SomeValue.Type
  type PersonAge = smithy4s.example.PersonAge.Type
  type TestString = smithy4s.example.TestString.Type
  type ObjectSize = smithy4s.example.ObjectSize.Type
  type Age = smithy4s.example.Age.Type
  type BucketName = smithy4s.example.BucketName.Type
  type Name = smithy4s.example.Name.Type
  type SomeIndexSeq = smithy4s.example.SomeIndexSeq.Type
  type ObjectKey = smithy4s.example.ObjectKey.Type
  type OrderNumber = smithy4s.example.OrderNumber.Type

}