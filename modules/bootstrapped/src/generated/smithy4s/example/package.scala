package smithy4s

package object example {
  type ErrorHandlingService[F[_]] = _root_.smithy4s.kinds.FunctorAlgebra[ErrorHandlingServiceGen, F]
  val ErrorHandlingService = ErrorHandlingServiceGen
  @deprecated(message = "N/A", since = "N/A")
  type DeprecatedService[F[_]] = _root_.smithy4s.kinds.FunctorAlgebra[DeprecatedServiceGen, F]
  val DeprecatedService = DeprecatedServiceGen
  type PackedInputsService[F[_]] = _root_.smithy4s.kinds.FunctorAlgebra[PackedInputsServiceGen, F]
  val PackedInputsService = PackedInputsServiceGen
  type StreamedObjects[F[_]] = _root_.smithy4s.kinds.FunctorAlgebra[StreamedObjectsGen, F]
  val StreamedObjects = StreamedObjectsGen
  type PizzaAdminService[F[_]] = _root_.smithy4s.kinds.FunctorAlgebra[PizzaAdminServiceGen, F]
  val PizzaAdminService = PizzaAdminServiceGen
  type FooService[F[_]] = _root_.smithy4s.kinds.FunctorAlgebra[FooServiceGen, F]
  val FooService = FooServiceGen
  type KVStore[F[_]] = _root_.smithy4s.kinds.FunctorAlgebra[KVStoreGen, F]
  val KVStore = KVStoreGen
  type ObjectService[F[_]] = _root_.smithy4s.kinds.FunctorAlgebra[ObjectServiceGen, F]
  val ObjectService = ObjectServiceGen
  type NameCollision[F[_]] = _root_.smithy4s.kinds.FunctorAlgebra[NameCollisionGen, F]
  val NameCollision = NameCollisionGen
  type ObjectCollision[F[_]] = _root_.smithy4s.kinds.FunctorAlgebra[ObjectCollisionGen, F]
  val ObjectCollision = ObjectCollisionGen
  type DummyService[F[_]] = _root_.smithy4s.kinds.FunctorAlgebra[DummyServiceGen, F]
  val DummyService = DummyServiceGen
  type EmptyService[F[_]] = _root_.smithy4s.kinds.FunctorAlgebra[EmptyServiceGen, F]
  val EmptyService = EmptyServiceGen
  type DiscriminatedService[F[_]] = _root_.smithy4s.kinds.FunctorAlgebra[DiscriminatedServiceGen, F]
  val DiscriminatedService = DiscriminatedServiceGen
  type Library[F[_]] = _root_.smithy4s.kinds.FunctorAlgebra[LibraryGen, F]
  val Library = LibraryGen
  type RecursiveInputService[F[_]] = _root_.smithy4s.kinds.FunctorAlgebra[RecursiveInputServiceGen, F]
  val RecursiveInputService = RecursiveInputServiceGen
  type BrandService[F[_]] = _root_.smithy4s.kinds.FunctorAlgebra[BrandServiceGen, F]
  val BrandService = BrandServiceGen
  type ErrorHandlingServiceExtraErrors[F[_]] = _root_.smithy4s.kinds.FunctorAlgebra[ErrorHandlingServiceExtraErrorsGen, F]
  val ErrorHandlingServiceExtraErrors = ErrorHandlingServiceExtraErrorsGen
  type Weather[F[_]] = _root_.smithy4s.kinds.FunctorAlgebra[WeatherGen, F]
  val Weather = WeatherGen

  type PublishersList = smithy4s.example.PublishersList.Type
  type TestString = smithy4s.example.TestString.Type
  type ArbitraryData = smithy4s.example.ArbitraryData.Type
  type PersonPhoneNumber = smithy4s.example.PersonPhoneNumber.Type
  type CSV = smithy4s.example.CSV.Type
  type CityId = smithy4s.example.CityId.Type
  type PersonAge = smithy4s.example.PersonAge.Type
  @deprecated(message = "N/A", since = "N/A")
  type DeprecatedString = smithy4s.example.DeprecatedString.Type
  type StringMap = smithy4s.example.StringMap.Type
  type ChanceOfRain = smithy4s.example.ChanceOfRain.Type
  /** @param key
    *   mapFoo
    * @param value
    *   mapBar
    */
  type MapWithMemberHints = smithy4s.example.MapWithMemberHints.Type
  type TestIdRefList = smithy4s.example.TestIdRefList.Type
  type CitySummaries = smithy4s.example.CitySummaries.Type
  /** This is a simple example of a "quoted string" */
  type AString = smithy4s.example.AString.Type
  type NonEmptyMapNumbers = smithy4s.example.NonEmptyMapNumbers.Type
  type StringSet = smithy4s.example.StringSet.Type
  type Menu = smithy4s.example.Menu.Type
  type Age = smithy4s.example.Age.Type
  type OrderNumber = smithy4s.example.OrderNumber.Type
  type FreeForm = smithy4s.example.FreeForm.Type
  type SomeInt = smithy4s.example.SomeInt.Type
  type DogName = smithy4s.example.DogName.Type
  type TestIdRefTwo = smithy4s.example.TestIdRefTwo.Type
  type FancyList = smithy4s.example.FancyList.Type
  type Ingredients = smithy4s.example.Ingredients.Type
  type StringList = smithy4s.example.StringList.Type
  /** Multiple line doc comment for another string
    * Containing a random \*\/ here.
    * Seriously, it's important to escape special characters.
    */
  type AnotherString = smithy4s.example.AnotherString.Type
  type PublisherId = smithy4s.example.PublisherId.Type
  type SomeValue = smithy4s.example.SomeValue.Type
  /** @param member
    *   listFoo
    */
  type ListWithMemberHints = smithy4s.example.ListWithMemberHints.Type
  type NonEmptyNames = smithy4s.example.NonEmptyNames.Type
  type BucketName = smithy4s.example.BucketName.Type
  type NonEmptyStrings = smithy4s.example.NonEmptyStrings.Type
  type TestStructurePattern = smithy4s.example.TestStructurePattern.Type
  type UnicodeRegexString = smithy4s.example.UnicodeRegexString.Type
  type UVIndex = smithy4s.example.UVIndex.Type
  @deprecated(message = "N/A", since = "N/A")
  type Strings = smithy4s.example.Strings.Type
  type NonEmptyCandies = smithy4s.example.NonEmptyCandies.Type
  type SomeIndexSeq = smithy4s.example.SomeIndexSeq.Type
  type TestIdRefSet = smithy4s.example.TestIdRefSet.Type
  type TestIdRefKeyMap = smithy4s.example.TestIdRefKeyMap.Type
  type StreamedBlob = smithy4s.example.StreamedBlob.Type
  type TestIdRefValueMap = smithy4s.example.TestIdRefValueMap.Type
  type PNG = smithy4s.example.PNG.Type
  type StringWithEnumTraits = smithy4s.example.StringWithEnumTraits.Type
  type Name = smithy4s.example.Name.Type
  type ObjectKey = smithy4s.example.ObjectKey.Type
  type UnwrappedFancyList = smithy4s.example.UnwrappedFancyList.Type
  type SparseStringList = smithy4s.example.SparseStringList.Type
  type SomeVector = smithy4s.example.SomeVector.Type
  type DefaultStringMap = smithy4s.example.DefaultStringMap.Type
  type SparseStringMap = smithy4s.example.SparseStringMap.Type
  type ObjectSize = smithy4s.example.ObjectSize.Type
  type PersonEmail = smithy4s.example.PersonEmail.Type

}