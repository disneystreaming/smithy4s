package smithy4s

package object example {
  val NAMESPACE: String = "smithy4s.example"

  type StreamedObjects[F[_]] = smithy4s.Monadic[StreamedObjectsGen, F]
  val StreamedObjects : smithy4s.Service[StreamedObjectsGen, StreamedObjectsOperation] = StreamedObjectsGen
  type FooService[F[_]] = smithy4s.Monadic[FooServiceGen, F]
  val FooService : smithy4s.Service[FooServiceGen, FooServiceOperation] = FooServiceGen
  type ObjectService[F[_]] = smithy4s.Monadic[ObjectServiceGen, F]
  val ObjectService : smithy4s.Service[ObjectServiceGen, ObjectServiceOperation] = ObjectServiceGen

  type ArbitraryData = smithy4s.example.ArbitraryData.Type
  type StreamedBlob = smithy4s.example.StreamedBlob.Type
  type SomeValue = smithy4s.example.SomeValue.Type
  type ObjectSize = smithy4s.example.ObjectSize.Type
  type BucketName = smithy4s.example.BucketName.Type
  type ObjectKey = smithy4s.example.ObjectKey.Type

}