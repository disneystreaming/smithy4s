package smithy4s

package object example {
  type StreamedObjects[F[_]] = smithy4s.kinds.FunctorAlgebra[StreamedObjectsGen, F]
  val StreamedObjects = StreamedObjectsGen
  type FooService[F[_]] = smithy4s.kinds.FunctorAlgebra[FooServiceGen, F]
  val FooService = FooServiceGen
  type BrandService[F[_]] = smithy4s.kinds.FunctorAlgebra[BrandServiceGen, F]
  val BrandService = BrandServiceGen
  type ObjectService[F[_]] = smithy4s.kinds.FunctorAlgebra[ObjectServiceGen, F]
  val ObjectService = ObjectServiceGen
  type NameCollision[F[_]] = smithy4s.kinds.FunctorAlgebra[NameCollisionGen, F]
  val NameCollision = NameCollisionGen
  @deprecated
  type DeprecatedService[F[_]] = smithy4s.kinds.FunctorAlgebra[DeprecatedServiceGen, F]
  val DeprecatedService = DeprecatedServiceGen

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
  @deprecated
  type Strings = smithy4s.example.Strings.Type
  type PersonAge = smithy4s.example.PersonAge.Type
  @deprecated
  type DeprecatedString = smithy4s.example.DeprecatedString.Type
  type ObjectSize = smithy4s.example.ObjectSize.Type
  type SomeIndexSeq = smithy4s.example.SomeIndexSeq.Type
  type StringList = smithy4s.example.StringList.Type

}