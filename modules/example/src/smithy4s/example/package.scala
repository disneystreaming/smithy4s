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
  @deprecated(message = "N/A", since = "N/A")
  type DeprecatedService[F[_]] = smithy4s.kinds.FunctorAlgebra[DeprecatedServiceGen, F]
  val DeprecatedService = DeprecatedServiceGen

  type StreamedBlob = smithy4s.example.StreamedBlob.Type
  /** This is a simple example of a "quoted string" */
  type AString = smithy4s.example.AString.Type
  type NonEmptyMapNumbers = smithy4s.example.NonEmptyMapNumbers.Type
  type SomeValue = smithy4s.example.SomeValue.Type
  type TestString = smithy4s.example.TestString.Type
  type NonEmptyNames = smithy4s.example.NonEmptyNames.Type
  type Age = smithy4s.example.Age.Type
  type BucketName = smithy4s.example.BucketName.Type
  type Name = smithy4s.example.Name.Type
  type NonEmptyStrings = smithy4s.example.NonEmptyStrings.Type
  type ObjectKey = smithy4s.example.ObjectKey.Type
  type OrderNumber = smithy4s.example.OrderNumber.Type
  type UnwrappedFancyList = smithy4s.example.UnwrappedFancyList.Type
  type TestStructurePattern = smithy4s.example.TestStructurePattern.Type
  type ArbitraryData = smithy4s.example.ArbitraryData.Type
  type DogName = smithy4s.example.DogName.Type
  type SomeVector = smithy4s.example.SomeVector.Type
  type PersonPhoneNumber = smithy4s.example.PersonPhoneNumber.Type
  type FancyList = smithy4s.example.FancyList.Type
  type DefaultStringMap = smithy4s.example.DefaultStringMap.Type
  @deprecated(message = "N/A", since = "N/A")
  type Strings = smithy4s.example.Strings.Type
  type PersonAge = smithy4s.example.PersonAge.Type
  @deprecated(message = "N/A", since = "N/A")
  type DeprecatedString = smithy4s.example.DeprecatedString.Type
  type ObjectSize = smithy4s.example.ObjectSize.Type
  type PersonEmail = smithy4s.example.PersonEmail.Type
  type NonEmptyCandies = smithy4s.example.NonEmptyCandies.Type
  type SomeIndexSeq = smithy4s.example.SomeIndexSeq.Type
  type StringList = smithy4s.example.StringList.Type
  /** Multiple line doc comment for another string
    * Containing a random \*\/ here.
    * Seriously, it's important to escape special characters.
    */
  type AnotherString = smithy4s.example.AnotherString.Type

}