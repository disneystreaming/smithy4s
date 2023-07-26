package smithy4s.example.collision

import smithy4s.Hints
import smithy4s.Schema
import smithy4s.ShapeId
import smithy4s.ShapeTag
import smithy4s.example._package.MyPackageString
import smithy4s.schema.Schema.struct

final case class TestReservedNamespaceImport(_package: Option[MyPackageString] = None)
object TestReservedNamespaceImport extends ShapeTag.Companion[TestReservedNamespaceImport] {

  val _package = MyPackageString.schema.optional[TestReservedNamespaceImport]("package", _._package, n => c => c.copy(_package = n))

  implicit val schema: Schema[TestReservedNamespaceImport] = struct(
    _package,
  ){
    TestReservedNamespaceImport.apply
  }
  .withId(ShapeId("smithy4s.example.collision", "TestReservedNamespaceImport"))
  .addHints(
    Hints.empty
  )
}
